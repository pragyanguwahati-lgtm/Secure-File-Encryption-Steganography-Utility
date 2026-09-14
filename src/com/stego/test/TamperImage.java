package com.stego.test;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class TamperImage {
    public static void main(String[] args) throws Exception {
        String inputPath = (args.length > 0) ? args[0] : "hidden.png";
        String outputPath = (args.length > 1) ? args[1] : "tampered.png";

        File file = new File(inputPath);
        if (!file.exists()) {
            System.err.println("[ERROR] File not found: " + inputPath);
            System.exit(1);
        }

        BufferedImage image = ImageIO.read(file);
        
        // Flip a bit in pixel (25, 0) which resides in the embedded payload
        int pixel = image.getRGB(25, 0);
        pixel = pixel ^ 1; // Flip LSB of blue channel
        image.setRGB(25, 0, pixel);
        
        ImageIO.write(image, "png", new File(outputPath));
        System.out.println("[SUCCESS] Created tampered image: " + outputPath);
    }
}
