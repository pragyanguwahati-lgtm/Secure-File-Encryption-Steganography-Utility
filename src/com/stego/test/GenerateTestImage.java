package com.stego.test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class GenerateTestImage {
    public static void main(String[] args) throws Exception {
        int width = 800;
        int height = 600;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(new Color(60, 120, 200));
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();
        ImageIO.write(image, "png", new File("test_image.png"));
        System.out.println("[SUCCESS] Created test_image.png (800x600)");
    }
}
