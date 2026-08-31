package com.workpulsetracker.agent.icons;

import com.workpulsetracker.agent.storage.LocalSqliteDatabase;
import com.workpulsetracker.agent.util.ApplicationNameNormalizer;
import com.workpulsetracker.agent.util.TrackedApplicationNameResolver;
import com.workpulsetracker.common.i18n.MessageCodes;
import com.workpulsetracker.common.i18n.Messages;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Кэш путей exe и системных иконок приложений для таблиц статистики.
 */
public final class ApplicationIconService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationIconService.class);
    private static final ApplicationIconService INSTANCE = new ApplicationIconService();
    private static final int ICON_SIZE = 16;
    private static final Color FALLBACK_BACKGROUND = new Color(0x1A, 0x1A, 0x2B);
    private static final Color FALLBACK_BORDER = new Color(0x74, 0x58, 0xFF);
    private static final String SELECT_ALL_EXECUTABLE_PATHS_SQL =
            "SELECT application_name, executable_path FROM executable_path";
    private static final String DELETE_ALL_EXECUTABLE_PATHS_SQL = "DELETE FROM executable_path";
    private static final String UPSERT_EXECUTABLE_PATH_SQL =
            "INSERT INTO executable_path (application_name, executable_path) VALUES (?, ?) "
                    + "ON CONFLICT(application_name) DO UPDATE SET executable_path = excluded.executable_path";

    private final ConcurrentHashMap<String, String> executablePathByApplicationName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ImageIcon> iconByApplicationName = new ConcurrentHashMap<>();
    private final ImageIcon fallbackIcon = createFallbackIcon();

    private ApplicationIconService() {
    }

    public static ApplicationIconService getInstance() {
        return INSTANCE;
    }

    public void load() {
        loadPersistedPaths();
        iconByApplicationName.clear();
        if (isWindows()) {
            WindowsProcessPathDiscovery.discoverRunningProcesses()
                    .forEach(this::rememberExecutablePathWithoutSave);
            saveQuietly();
        }
        Thread iconPreloadThread = new Thread(this::preloadIcons, "application-icon-preload");
        iconPreloadThread.setDaemon(true);
        iconPreloadThread.start();
    }

    private void loadPersistedPaths() {
        try {
            Map<String, String> loadedExecutablePaths =
                    LocalSqliteDatabase.getInstance().call(ApplicationIconService::loadExecutablePathsFromConnection);
            executablePathByApplicationName.clear();
            loadedExecutablePaths.forEach(executablePathByApplicationName::put);
        } catch (SQLException exception) {
            logger.warn("schema=local Failed to load executable paths: {}", exception.getMessage());
        }
    }

    public void rememberExecutablePath(String applicationName, String executablePath) {
        if (rememberExecutablePathWithoutSave(applicationName, executablePath)) {
            saveQuietly();
        }
    }

    private boolean rememberExecutablePathWithoutSave(String applicationName, String executablePath) {
        if (StringUtils.isBlank(applicationName) || StringUtils.isBlank(executablePath)) {
            return false;
        }
        String normalizedApplicationName = ApplicationNameNormalizer.normalize(applicationName);
        String normalizedExecutablePath = executablePath.trim();
        String previousExecutablePath = executablePathByApplicationName.put(
                normalizedApplicationName,
                normalizedExecutablePath
        );
        if (!Objects.equals(previousExecutablePath, normalizedExecutablePath)) {
            iconByApplicationName.remove(normalizedApplicationName);
            return true;
        }
        return false;
    }

    public ImageIcon getIcon(String applicationName) {
        if (StringUtils.isBlank(applicationName) || isOthersCategory(applicationName)) {
            return fallbackIcon;
        }
        String baseApplicationName = TrackedApplicationNameResolver.extractBaseApplicationName(applicationName);
        ImageIcon cachedIcon = iconByApplicationName.get(baseApplicationName);
        if (Objects.nonNull(cachedIcon)) {
            return cachedIcon;
        }
        ImageIcon loadedIcon = loadIcon(baseApplicationName);
        iconByApplicationName.put(baseApplicationName, loadedIcon);
        return loadedIcon;
    }

    /**
     * Известный путь к exe для приложения (если уже запомнен).
     */
    public String findExecutablePath(String applicationName) {
        if (StringUtils.isBlank(applicationName)) {
            return null;
        }
        String baseApplicationName = TrackedApplicationNameResolver.extractBaseApplicationName(applicationName);
        return executablePathByApplicationName.get(ApplicationNameNormalizer.normalize(baseApplicationName));
    }

    private void preloadIcons() {
        executablePathByApplicationName.keySet().forEach(this::getIcon);
    }

    private ImageIcon loadIcon(String applicationName) {
        String executablePath = executablePathByApplicationName.get(applicationName);
        if (StringUtils.isBlank(executablePath)) {
            return fallbackIcon;
        }
        File executableFile = Path.of(executablePath).toFile();
        if (!executableFile.isFile()) {
            return fallbackIcon;
        }
        try {
            ImageIcon shellFolderIcon = loadIconViaShellFolder(executableFile);
            if (Objects.nonNull(shellFolderIcon)) {
                return shellFolderIcon;
            }
            ImageIcon sizedSystemIcon = loadIconViaSizedSystemIcon(executableFile);
            if (Objects.nonNull(sizedSystemIcon)) {
                return sizedSystemIcon;
            }
            Icon systemIcon = FileSystemView.getFileSystemView().getSystemIcon(executableFile);
            if (Objects.isNull(systemIcon)) {
                return fallbackIcon;
            }
            return scaleIcon(systemIcon);
        } catch (Exception exception) {
            logger.debug("Failed to load icon for {}: {}", applicationName, exception.getMessage());
            return fallbackIcon;
        }
    }

    private ImageIcon loadIconViaSizedSystemIcon(File executableFile) {
        try {
            Method getSystemIconMethod = FileSystemView.class.getMethod(
                    "getSystemIcon",
                    File.class,
                    int.class,
                    int.class
            );
            Object iconObject = getSystemIconMethod.invoke(
                    FileSystemView.getFileSystemView(),
                    executableFile,
                    ICON_SIZE,
                    ICON_SIZE
            );
            if (iconObject instanceof ImageIcon imageIcon && Objects.nonNull(imageIcon.getImage())) {
                return imageIcon;
            }
            if (iconObject instanceof Icon icon) {
                return scaleIcon(icon);
            }
        } catch (ReflectiveOperationException ignored) {
            // Java 8 FileSystemView does not have sized overload.
        }
        return null;
    }

    private ImageIcon loadIconViaShellFolder(File executableFile) {
        try {
            Class<?> shellFolderClass = Class.forName("sun.awt.shell.ShellFolder");
            Method getShellFolderMethod = shellFolderClass.getMethod("getShellFolder", File.class);
            Object shellFolder = getShellFolderMethod.invoke(null, executableFile);
            Method getIconMethod = shellFolderClass.getMethod("getIcon", boolean.class);
            Object imageObject = getIconMethod.invoke(shellFolder, Boolean.TRUE);
            if (imageObject instanceof Image image) {
                return new ImageIcon(image.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH));
            }
        } catch (ReflectiveOperationException ignored) {
            // Internal API may be unavailable.
        }
        return null;
    }

    private ImageIcon scaleIcon(Icon icon) {
        int sourceWidth = Math.max(icon.getIconWidth(), 1);
        int sourceHeight = Math.max(icon.getIconHeight(), 1);
        BufferedImage sourceImage = new BufferedImage(sourceWidth, sourceHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D sourceGraphics = sourceImage.createGraphics();
        try {
            sourceGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            icon.paintIcon(null, sourceGraphics, 0, 0);
        } finally {
            sourceGraphics.dispose();
        }

        BufferedImage scaledImage = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D scaledGraphics = scaledImage.createGraphics();
        try {
            scaledGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            scaledGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            scaledGraphics.drawImage(sourceImage, 0, 0, ICON_SIZE, ICON_SIZE, null);
        } finally {
            scaledGraphics.dispose();
        }
        return new ImageIcon(scaledImage);
    }

    private ImageIcon createFallbackIcon() {
        BufferedImage bufferedImage = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        try {
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(FALLBACK_BACKGROUND);
            graphics2D.fillRoundRect(1, 1, ICON_SIZE - 2, ICON_SIZE - 2, 4, 4);
            graphics2D.setColor(FALLBACK_BORDER);
            graphics2D.drawRoundRect(1, 1, ICON_SIZE - 2, ICON_SIZE - 2, 4, 4);
        } finally {
            graphics2D.dispose();
        }
        return new ImageIcon(bufferedImage);
    }

    private boolean isOthersCategory(String applicationName) {
        return Objects.equals(applicationName, Messages.get(MessageCodes.UI_STATS_OTHERS))
                || Objects.equals(applicationName, "Others")
                || Objects.equals(applicationName, "Інші");
    }

    private void saveQuietly() {
        try {
            LocalSqliteDatabase.getInstance().run(
                    connection -> replaceAllExecutablePathsOnConnection(connection, executablePathByApplicationName)
            );
        } catch (SQLException exception) {
            logger.warn("schema=local Failed to save executable paths: {}", exception.getMessage());
        }
    }

    public static void replaceAllExecutablePathsOnConnection(
            Connection connection,
            Map<String, String> executablePathByApplicationName
    ) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (PreparedStatement deleteStatement = connection.prepareStatement(DELETE_ALL_EXECUTABLE_PATHS_SQL);
             PreparedStatement upsertStatement = connection.prepareStatement(UPSERT_EXECUTABLE_PATH_SQL)) {
            deleteStatement.executeUpdate();
            executablePathByApplicationName.entrySet().stream()
                    .filter(entry -> StringUtils.isNotBlank(entry.getKey()) && StringUtils.isNotBlank(entry.getValue()))
                    .forEach(entry -> {
                        try {
                            upsertStatement.setString(1, ApplicationNameNormalizer.normalize(entry.getKey()));
                            upsertStatement.setString(2, entry.getValue().trim());
                            upsertStatement.addBatch();
                        } catch (SQLException exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
            upsertStatement.executeBatch();
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private static Map<String, String> loadExecutablePathsFromConnection(Connection connection) throws SQLException {
        ConcurrentHashMap<String, String> loadedExecutablePaths = new ConcurrentHashMap<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_EXECUTABLE_PATHS_SQL);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                String applicationName = resultSet.getString("application_name");
                String executablePath = resultSet.getString("executable_path");
                if (StringUtils.isBlank(applicationName) || StringUtils.isBlank(executablePath)) {
                    continue;
                }
                loadedExecutablePaths.put(ApplicationNameNormalizer.normalize(applicationName), executablePath.trim());
            }
        }
        return loadedExecutablePaths;
    }

    private static boolean isWindows() {
        String operatingSystemName = System.getProperty("os.name", "");
        return operatingSystemName.toLowerCase(Locale.ROOT).contains("win");
    }
}
