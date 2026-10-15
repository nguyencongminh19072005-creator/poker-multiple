import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

/** Rebuilds the transparent, gently breathing dealer GIF from the original artwork. */
public final class GenerateDealerIdleGif {
    private static final int WIDTH = 256;
    private static final int HEIGHT = 384;
    private static final int FRAMES = 24;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: java tools/GenerateDealerIdleGif.java input.png output.gif");
        }
        BufferedImage source = ImageIO.read(Path.of(args[0]).toFile());
        if (source == null) throw new IllegalArgumentException("Cannot read PNG: " + args[0]);
        Path output = Path.of(args[1]);
        Files.createDirectories(output.getParent());

        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output.toFile())) {
            writer.setOutput(stream);
            writer.prepareWriteSequence(null);
            for (int frame = 0; frame < FRAMES; frame++) {
                double phase = 2 * Math.PI * frame / FRAMES;
                BufferedImage picture = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = picture.createGraphics();
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);

                // Pivot near the feet: a small breath and sway, no dealing gesture.
                AffineTransform motion = new AffineTransform();
                motion.translate(WIDTH / 2.0, HEIGHT - 1);
                motion.rotate(Math.sin(phase) * 0.006);
                motion.scale(1 + Math.sin(phase) * 0.006,
                        1 + Math.sin(phase - Math.PI / 2) * 0.012);
                motion.translate(-WIDTH / 2.0, -HEIGHT + 1);
                graphics.transform(motion);
                graphics.drawImage(source, 0, 0, WIDTH, HEIGHT, null);
                graphics.dispose();

                BufferedImage indexed = indexForGif(picture);
                IIOMetadata metadata = writer.getDefaultImageMetadata(
                        ImageTypeSpecifier.createFromRenderedImage(indexed), params);
                String format = metadata.getNativeMetadataFormatName();
                IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
                IIOMetadataNode control = (IIOMetadataNode) root.getElementsByTagName(
                        "GraphicControlExtension").item(0);
                control.setAttribute("disposalMethod", "none");
                control.setAttribute("delayTime", "10"); // 100 ms per frame, 2.4 s per loop.
                control.setAttribute("transparentColorFlag", "TRUE");
                control.setAttribute("transparentColorIndex", "0");
                if (frame == 0) {
                    IIOMetadataNode extensions = new IIOMetadataNode("ApplicationExtensions");
                    IIOMetadataNode loop = new IIOMetadataNode("ApplicationExtension");
                    loop.setAttribute("applicationID", "NETSCAPE");
                    loop.setAttribute("authenticationCode", "2.0");
                    loop.setUserObject(new byte[] {1, 0, 0}); // Repeat forever.
                    extensions.appendChild(loop);
                    root.appendChild(extensions);
                }
                metadata.setFromTree(format, root);
                writer.writeToSequence(new IIOImage(indexed, null, metadata), params);
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }

    private static BufferedImage indexForGif(BufferedImage picture) {
        byte[] red = new byte[253];
        byte[] green = new byte[253];
        byte[] blue = new byte[253];
        byte[] alpha = new byte[253];
        for (int r = 0; r < 6; r++) {
            for (int g = 0; g < 7; g++) {
                for (int b = 0; b < 6; b++) {
                    int index = 1 + (r * 7 + g) * 6 + b;
                    red[index] = (byte) (r * 255 / 5);
                    green[index] = (byte) (g * 255 / 6);
                    blue[index] = (byte) (b * 255 / 5);
                    alpha[index] = (byte) 255;
                }
            }
        }
        IndexColorModel palette = new IndexColorModel(8, 253, red, green, blue, alpha);
        BufferedImage indexed = new BufferedImage(WIDTH, HEIGHT,
                BufferedImage.TYPE_BYTE_INDEXED, palette);
        WritableRaster raster = indexed.getRaster();
        double[][] errors = new double[WIDTH + 2][3];
        double[][] nextErrors = new double[WIDTH + 2][3];
        for (int y = 0; y < HEIGHT; y++) {
            boolean reverse = y % 2 == 1;
            for (int x = 0; x < WIDTH; x++) {
                int column = reverse ? WIDTH - x - 1 : x;
                int argb = picture.getRGB(column, y);
                int a = (argb >>> 24) & 0xff;
                if (a < 100) continue;
                int[] value = {
                        clamp(((argb >>> 16) & 0xff) + errors[column + 1][0]),
                        clamp(((argb >>> 8) & 0xff) + errors[column + 1][1]),
                        clamp((argb & 0xff) + errors[column + 1][2])
                };
                int r = (value[0] * 5 + 127) / 255;
                int g = (value[1] * 6 + 127) / 255;
                int b = (value[2] * 5 + 127) / 255;
                raster.setSample(column, y, 0, 1 + (r * 7 + g) * 6 + b);
                int[] difference = {value[0] - r * 255 / 5,
                        value[1] - g * 255 / 6, value[2] - b * 255 / 5};
                int direction = reverse ? -1 : 1;
                for (int channel = 0; channel < 3; channel++) {
                    errors[column + 1 + direction][channel] += difference[channel] * 7.0 / 16;
                    nextErrors[column + 1 - direction][channel] += difference[channel] * 3.0 / 16;
                    nextErrors[column + 1][channel] += difference[channel] * 5.0 / 16;
                    nextErrors[column + 1 + direction][channel] += difference[channel] / 16.0;
                }
            }
            double[][] oldErrors = errors;
            errors = nextErrors;
            nextErrors = oldErrors;
            for (double[] channels : nextErrors) java.util.Arrays.fill(channels, 0);
        }
        return indexed;
    }

    private static int clamp(double value) {
        return (int) Math.max(0, Math.min(255, Math.round(value)));
    }
}
