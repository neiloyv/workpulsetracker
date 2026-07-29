package com.workpulsetracker.agent.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Загрузка брендовых изображений агента из classpath.
 */
public final class UiImages {

    private static final Logger logger = LoggerFactory.getLogger(UiImages.class);

    private static final String LOGO_RESOURCE_PATH = "/images/workpulse-logo.png";
    private static final String ICON_RESOURCE_PATH = "/images/workpulse-icon.png";
    private static final int BLACK_TRANSPARENCY_THRESHOLD = 18;

    private UiImages() {
    }

    public static ImageIcon loadLogoIcon(int targetHeight) {
        BufferedImage logoImage = loadImage(LOGO_RESOURCE_PATH);
        if (Objects.isNull(logoImage)) {
            return null;
        }
        return new ImageIcon(scaleToHeight(logoImage, targetHeight));
    }

    public static Image loadTrayIconImage(int size) {
        BufferedImage iconImage = loadImage(ICON_RESOURCE_PATH);
        if (Objects.isNull(iconImage)) {
            return createFallbackTrayImage(size);
        }
        return scaleToSquare(makeNearBlackTransparent(iconImage), size);
    }

    public static List<Image> loadWindowIconImages() {
        BufferedImage iconImage = loadImage(ICON_RESOURCE_PATH);
        if (Objects.isNull(iconImage)) {
            return List.of(createFallbackTrayImage(32));
        }
        BufferedImage transparentIconImage = makeNearBlackTransparent(iconImage);
        return Stream.of(16, 24, 32, 48, 64)
                .map(size -> scaleToSquare(transparentIconImage, size))
                .collect(Collectors.toList());
    }

    private static BufferedImage loadImage(String resourcePath) {
        try (InputStream inputStream = UiImages.class.getResourceAsStream(resourcePath)) {
            if (Objects.isNull(inputStream)) {
                logger.warn("Image resource not found: {}", resourcePath);
                return null;
            }
            return ImageIO.read(inputStream);
        } catch (IOException exception) {
            logger.warn("Failed to load image {}: {}", resourcePath, exception.getMessage());
            return null;
        }
    }

    /**
     * Убирает чёрный фон у иконки щита, чтобы в трее/тайтлбаре оставался только логотип.
     */
    private static BufferedImage makeNearBlackTransparent(BufferedImage sourceImage) {
        BufferedImage transparentImage = new BufferedImage(
                sourceImage.getWidth(),
                sourceImage.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        for (int x = 0; x < sourceImage.getWidth(); x++) {
            for (int y = 0; y < sourceImage.getHeight(); y++) {
                int rgb = sourceImage.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xFF;
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                if (red <= BLACK_TRANSPARENCY_THRESHOLD
                        && green <= BLACK_TRANSPARENCY_THRESHOLD
                        && blue <= BLACK_TRANSPARENCY_THRESHOLD) {
                    transparentImage.setRGB(x, y, 0x00000000);
                } else {
                    transparentImage.setRGB(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
                }
            }
        }
        return transparentImage;
    }

    private static Image scaleToHeight(BufferedImage sourceImage, int targetHeight) {
        int safeHeight = Math.max(targetHeight, 1);
        int targetWidth = Math.max(
                1,
                (int) Math.round(sourceImage.getWidth() * (safeHeight / (double) sourceImage.getHeight()))
        );
        return scaleImage(sourceImage, targetWidth, safeHeight);
    }

    private static Image scaleToSquare(BufferedImage sourceImage, int size) {
        int safeSize = Math.max(size, 1);
        return scaleImage(sourceImage, safeSize, safeSize);
    }

    private static BufferedImage scaleImage(BufferedImage sourceImage, int targetWidth, int targetHeight) {
        BufferedImage scaledImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = scaledImage.createGraphics();
        try {
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics2D.dispose();
        }
        return scaledImage;
    }

    private static Image createFallbackTrayImage(int size) {
        int safeSize = Math.max(size, 1);
        BufferedImage bufferedImage = new BufferedImage(safeSize, safeSize, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < safeSize; x++) {
            for (int y = 0; y < safeSize; y++) {
                boolean border = x == 0 || y == 0 || x == safeSize - 1 || y == safeSize - 1;
                bufferedImage.setRGB(x, y, border ? 0xFF7458FF : 0xFF0A0A14);
            }
        }
        return bufferedImage;
    }
}
