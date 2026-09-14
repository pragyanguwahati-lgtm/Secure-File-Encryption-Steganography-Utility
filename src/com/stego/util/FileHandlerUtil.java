package com.stego.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FileHandlerUtil {

    public static byte[] readFileToBytes(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new Exception("File not found: " + filePath);
        }
        if (file.isDirectory()) {
            throw new Exception("Specified path is a directory, expected a file: " + filePath);
        }
        byte[] data = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead = 0;
            while (bytesRead < data.length) {
                int read = fis.read(data, bytesRead, data.length - bytesRead);
                if (read == -1) break;
                bytesRead += read;
            }
        }
        return data;
    }

    public static void writeBytesToFile(String filePath, byte[] data) throws Exception {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }

    public static BufferedImage readImage(String imagePath) throws Exception {
        File file = new File(imagePath);
        if (!file.exists()) {
            throw new Exception("Image file not found: " + imagePath);
        }
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            throw new Exception("Failed to decode image file (unsupported format or corrupted): " + imagePath);
        }
        return image;
    }

    public static void writeImage(BufferedImage image, String format, String outputPath) throws Exception {
        File file = new File(outputPath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        boolean success = ImageIO.write(image, format, file);
        if (!success) {
            throw new Exception("No appropriate image writer found for format: " + format);
        }
    }
}
