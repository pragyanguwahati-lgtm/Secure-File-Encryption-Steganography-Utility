import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class TamperImage {
    public static void main(String[] args) throws Exception {
        BufferedImage image = ImageIO.read(new File("hidden.png"));
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Flip a bit in the middle of the image
        int x = width / 2;
        int y = height / 2;
        int pixel = image.getRGB(x, y);
        pixel = pixel ^ 1; // Flip LSB of blue channel
        image.setRGB(x, y, pixel);
        
        ImageIO.write(image, "png", new File("tampered.png"));
        System.out.println("Created tampered.png");
    }
}
