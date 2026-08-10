import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Конвертирует PNG в multi-size .ico (PNG entries, Windows Vista+).
 * Запуск: java GenerateWindowsIcon.java app-icon.png app-icon.ico
 */
public final class GenerateWindowsIcon {

    private static final int[] ICON_SIZES = {16, 32, 48, 256};

    private GenerateWindowsIcon() {
    }

    public static void main(String[] arguments) throws IOException {
        if (arguments.length < 2) {
            System.err.println("Usage: java GenerateWindowsIcon.java <input.png> <output.ico>");
            System.exit(1);
        }

        BufferedImage sourceImage = ImageIO.read(new File(arguments[0]));
        if (sourceImage == null) {
            throw new IOException("Unsupported or missing PNG: " + arguments[0]);
        }

        List<byte[]> pngImages = new ArrayList<>();
        List<Integer> iconSizes = new ArrayList<>();
        for (int iconSize : ICON_SIZES) {
            pngImages.add(toPngBytes(scaleToSquare(sourceImage, iconSize)));
            iconSizes.add(iconSize);
        }

        Files.write(new File(arguments[1]).toPath(), buildIco(pngImages, iconSizes));
        System.out.println("Generated ICO: " + arguments[1] + " (" + ICON_SIZES.length + " sizes)");
    }

    private static BufferedImage scaleToSquare(BufferedImage sourceImage, int targetSize) {
        BufferedImage scaledImage = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaledImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(sourceImage, 0, 0, targetSize, targetSize, null);
        graphics.dispose();
        return scaledImage;
    }

    private static byte[] toPngBytes(BufferedImage bufferedImage) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (!ImageIO.write(bufferedImage, "png", outputStream)) {
            throw new IOException("Failed to encode PNG image");
        }
        return outputStream.toByteArray();
    }

    private static byte[] buildIco(List<byte[]> pngImages, List<Integer> iconSizes) {
        int imageCount = pngImages.size();
        int headerSize = 6 + (imageCount * 16);
        int dataOffset = headerSize;

        ByteBuffer byteBuffer = ByteBuffer.allocate(headerSize + pngImages.stream().mapToInt(image -> image.length).sum());
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        byteBuffer.putShort((short) 0);
        byteBuffer.putShort((short) 1);
        byteBuffer.putShort((short) imageCount);

        for (int index = 0; index < imageCount; index++) {
            int iconSize = iconSizes.get(index);
            byte[] pngImage = pngImages.get(index);
            byteBuffer.put((byte) (iconSize >= 256 ? 0 : iconSize));
            byteBuffer.put((byte) (iconSize >= 256 ? 0 : iconSize));
            byteBuffer.put((byte) 0);
            byteBuffer.put((byte) 0);
            byteBuffer.putShort((short) 1);
            byteBuffer.putShort((short) 32);
            byteBuffer.putInt(pngImage.length);
            byteBuffer.putInt(dataOffset);
            dataOffset += pngImage.length;
        }

        for (byte[] pngImage : pngImages) {
            byteBuffer.put(pngImage);
        }

        return byteBuffer.array();
    }
}
