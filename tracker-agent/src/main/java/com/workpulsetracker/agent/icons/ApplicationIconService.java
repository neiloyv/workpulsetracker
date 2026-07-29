package com.workpulsetracker.agent.icons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.workpulsetracker.agent.storage.LocalDataDirectory;
import com.workpulsetracker.agent.util.ApplicationNameNormalizer;
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
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private static final Type PATH_MAP_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();
    private static final int ICON_SIZE = 16;
    private static final Color FALLBACK_BACKGROUND = new Color(0x1A, 0x1A, 0x2B);
    private static final Color FALLBACK_BORDER = new Color(0x74, 0x58, 0xFF);

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ConcurrentHashMap<String, String> executablePathByApplicationName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ImageIcon> iconByApplicationName = new ConcurrentHashMap<>();
    private final ImageIcon fallbackIcon = createFallbackIcon();
    private final Path executablePathsFilePath;

    private ApplicationIconService() {
        this(LocalDataDirectory.getExecutablePathsFilePath());
    }

    ApplicationIconService(Path executablePathsFilePath) {
        this.executablePathsFilePath = executablePathsFilePath;
    }

    public static ApplicationIconService getInstance() {
        return INSTANCE;
    }

    public void load() {
        loadPersistedPaths();
        if (isWindows()) {
            WindowsProcessPathDiscovery.discoverRunningProcesses()
                    .forEach(this::rememberExecutablePathWithoutSave);
            saveQuietly();
        }
        preloadIcons();
    }

    private void loadPersistedPaths() {
        try {
            Files.createDirectories(executablePathsFilePath.getParent());
            if (!Files.exists(executablePathsFilePath)) {
                return;
            }
            try (Reader reader = Files.newBufferedReader(executablePathsFilePath, StandardCharsets.UTF_8)) {
                Map<String, String> loadedExecutablePaths = gson.fromJson(reader, PATH_MAP_TYPE);
                if (Objects.isNull(loadedExecutablePaths)) {
                    return;
                }
                loadedExecutablePaths.entrySet().stream()
                        .filter(entry -> StringUtils.isNotBlank(entry.getKey()) && StringUtils.isNotBlank(entry.getValue()))
                        .forEach(entry -> executablePathByApplicationName.put(
                                ApplicationNameNormalizer.normalize(entry.getKey()),
                                entry.getValue().trim()
                        ));
            }
        } catch (IOException exception) {
            logger.warn("Failed to load executable-paths.json: {}", exception.getMessage());
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
        String normalizedApplicationName = ApplicationNameNormalizer.normalize(applicationName);
        ImageIcon cachedIcon = iconByApplicationName.get(normalizedApplicationName);
        if (Objects.nonNull(cachedIcon)) {
            return cachedIcon;
        }
        ImageIcon loadedIcon = loadIcon(normalizedApplicationName);
        iconByApplicationName.put(normalizedApplicationName, loadedIcon);
        return loadedIcon;
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
            Files.createDirectories(executablePathsFilePath.getParent());
            try (Writer writer = Files.newBufferedWriter(executablePathsFilePath, StandardCharsets.UTF_8)) {
                gson.toJson(executablePathByApplicationName, writer);
            }
        } catch (IOException exception) {
            logger.warn("Failed to save executable-paths.json: {}", exception.getMessage());
        }
    }

    private static boolean isWindows() {
        String operatingSystemName = System.getProperty("os.name", "");
        return operatingSystemName.toLowerCase(Locale.ROOT).contains("win");
    }
}
