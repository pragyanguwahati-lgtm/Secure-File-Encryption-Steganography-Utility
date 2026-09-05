import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileHandlerUtil {

    public static byte[] readFileToBytes(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.readAllBytes(path);
    }

    public static void writeBytesToFile(String filePath, byte[] data) throws IOException {
        Path path = Paths.get(filePath);
        Files.write(path, data);
    }

    public static BufferedImage readImage(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Image file does not exist: " + filePath);
        }
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new IOException("Failed to read image or unsupported format: " + filePath);
        }
        return image;
    }

    public static void writeImage(BufferedImage image, String formatName, String filePath) throws IOException {
        File file = new File(filePath);
        boolean success = ImageIO.write(image, formatName, file);
        if (!success) {
            throw new IOException("Failed to write image to format " + formatName);
        }
    }
}
