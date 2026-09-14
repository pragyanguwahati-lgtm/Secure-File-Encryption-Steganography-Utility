package com.stego.cli;

import com.stego.core.StegoService;
import com.stego.crypto.CryptoService;
import com.stego.crypto.IntegrityValidator;
import com.stego.exception.CapacityExceededException;
import com.stego.exception.InvalidPasswordException;
import com.stego.exception.TamperedFileException;
import com.stego.util.FileHandlerUtil;

import java.awt.image.BufferedImage;
import java.io.Console;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;

public class MainCLI {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String mode = args[0].toLowerCase();
        try {
            switch (mode) {
                case "--hide":
                case "-h":
                    handleHide(args);
                    break;
                case "--extract":
                case "-x":
                case "-e":
                    handleExtract(args);
                    break;
                case "--info":
                case "--analyze":
                case "-a":
                    handleInfo(args);
                    break;
                case "--help":
                    printUsage();
                    break;
                default:
                    System.err.println("[ERROR] Unknown mode: " + mode);
                    printUsage();
                    System.exit(1);
            }
        } catch (InvalidPasswordException e) {
            System.err.println("[ERROR] " + e.getMessage());
            System.exit(1);
        } catch (TamperedFileException e) {
            System.err.println("[ERROR] Integrity check failed: " + e.getMessage());
            System.exit(1);
        } catch (CapacityExceededException e) {
            System.err.println("[ERROR] Capacity error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("[ERROR] Operation failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void handleHide(String[] args) throws Exception {
        String targetPath = null;
        String coverPath = null;
        String outPath = null;

        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if ((arg.equals("--target") || arg.equals("-f") || arg.equals("-t")) && i + 1 < args.length) {
                targetPath = args[++i];
            } else if ((arg.equals("--cover") || arg.equals("-i") || arg.equals("-c")) && i + 1 < args.length) {
                coverPath = args[++i];
            } else if ((arg.equals("--out") || arg.equals("-o")) && i + 1 < args.length) {
                outPath = args[++i];
            }
        }

        if (targetPath == null || coverPath == null || outPath == null) {
            System.err.println("[ERROR] Missing required arguments for hiding.");
            System.err.println("Usage: java -jar SecTool.jar --hide -f <target_file> -i <cover_image> -o <out_image>");
            System.exit(1);
        }

        System.out.println("[INFO] Reading target file...");
        byte[] fileData = FileHandlerUtil.readFileToBytes(targetPath);

        System.out.println("[INFO] Generating SHA-256 integrity hash...");
        byte[] hash = IntegrityValidator.generateSHA256(fileData);

        // Plaintext: [32 bytes SHA-256 hash] + [file contents]
        ByteBuffer plaintextBuffer = ByteBuffer.allocate(hash.length + fileData.length);
        plaintextBuffer.put(hash);
        plaintextBuffer.put(fileData);
        byte[] plaintext = plaintextBuffer.array();

        char[] password = readPassword("Enter encryption password: ");
        byte[] encryptedPayload;
        try {
            System.out.println("[INFO] Encrypting data with AES-256...");
            encryptedPayload = CryptoService.encrypt(plaintext, password);
        } finally {
            Arrays.fill(password, '\0');
        }

        System.out.println("[INFO] Reading cover image...");
        BufferedImage coverImage = FileHandlerUtil.readImage(coverPath);

        System.out.println("[INFO] Embedding encrypted payload into image...");
        BufferedImage stegoImage = StegoService.embed(coverImage, encryptedPayload);

        System.out.println("[INFO] Saving stego image...");
        FileHandlerUtil.writeImage(stegoImage, "png", outPath);

        System.out.println("[SUCCESS] Data hidden successfully in " + outPath);
    }

    private static void handleExtract(String[] args) throws Exception {
        String imagePath = null;
        String outPath = null;

        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if ((arg.equals("--image") || arg.equals("-i") || arg.equals("-c") || arg.equals("--cover")) && i + 1 < args.length) {
                imagePath = args[++i];
            } else if ((arg.equals("--out") || arg.equals("-o")) && i + 1 < args.length) {
                outPath = args[++i];
            }
        }

        if (imagePath == null || outPath == null) {
            System.err.println("[ERROR] Missing required arguments for extraction.");
            System.err.println("Usage: java -jar SecTool.jar --extract -i <stego_image> -o <out_file>");
            System.exit(1);
        }

        System.out.println("[INFO] Reading stego image...");
        BufferedImage stegoImage = FileHandlerUtil.readImage(imagePath);

        System.out.println("[INFO] Extracting payload from image...");
        byte[] encryptedPayload = StegoService.extract(stegoImage);

        char[] password = readPassword("Enter decryption password: ");
        byte[] decryptedData;
        try {
            System.out.println("[INFO] Decrypting data...");
            decryptedData = CryptoService.decrypt(encryptedPayload, password);
        } finally {
            Arrays.fill(password, '\0');
        }

        if (decryptedData.length < 32) {
            throw new Exception("Decrypted payload is corrupted (smaller than SHA-256 hash length).");
        }

        System.out.println("[INFO] Verifying SHA-256 integrity hash...");
        byte[] embeddedHash = Arrays.copyOfRange(decryptedData, 0, 32);
        byte[] originalData = Arrays.copyOfRange(decryptedData, 32, decryptedData.length);

        if (!IntegrityValidator.verify(embeddedHash, originalData)) {
            throw new TamperedFileException("Hash verification failed. The payload has been tampered with or corrupted!");
        }

        System.out.println("[INFO] Writing restored file...");
        FileHandlerUtil.writeBytesToFile(outPath, originalData);

        System.out.println("[SUCCESS] Hash matched. Data extracted to " + outPath);
    }

    private static void handleInfo(String[] args) throws Exception {
        String imagePath = null;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if ((arg.equals("--image") || arg.equals("-i") || arg.equals("-c") || arg.equals("--cover")) && i + 1 < args.length) {
                imagePath = args[++i];
            }
        }

        if (imagePath == null) {
            System.err.println("[ERROR] Missing image path.");
            System.err.println("Usage: java -jar SecTool.jar --info -i <image.png>");
            System.exit(1);
        }

        BufferedImage img = FileHandlerUtil.readImage(imagePath);
        int w = img.getWidth();
        int h = img.getHeight();
        long totalPixels = (long) w * h;
        long totalCapacityBytes = (totalPixels * 3L) / 8L;
        long usablePayloadBytes = StegoService.getPayloadCapacityBytes(img);
        boolean hasSteg = StegoService.hasValidSignature(img);

        System.out.println("=================================================");
        System.out.println("           COVER IMAGE ANALYSIS REPORT           ");
        System.out.println("=================================================");
        System.out.printf(" Image Path          : %s%n", imagePath);
        System.out.printf(" Dimensions          : %d x %d pixels%n", w, h);
        System.out.printf(" Total Pixels        : %,d%n", totalPixels);
        System.out.printf(" Total LSB Capacity  : %,d bytes (%.2f KB)%n", totalCapacityBytes, totalCapacityBytes / 1024.0);
        System.out.printf(" Usable Payload Max  : %,d bytes (%.2f KB)%n", usablePayloadBytes, usablePayloadBytes / 1024.0);
        System.out.printf(" Stego Signature     : %s%n", (hasSteg ? "[FOUND] Valid STEG payload present" : "[NONE] Clean cover image"));
        System.out.println("=================================================");
    }

    private static char[] readPassword(String prompt) {
        Console console = System.console();
        if (console != null) {
            return console.readPassword(prompt);
        } else {
            System.out.print(prompt);
            Scanner scanner = new Scanner(System.in);
            String line = scanner.nextLine();
            return line.toCharArray();
        }
    }

    private static void printUsage() {
        System.out.println("Secure File Encryption & Steganography Utility (SecTool)");
        System.out.println("---------------------------------------------------------");
        System.out.println("Usage:");
        System.out.println("  Hide data:");
        System.out.println("    java -jar SecTool.jar --hide -f <target_file> -i <cover_image> -o <out_image>");
        System.out.println("    (Aliases: --target, --cover, --out)");
        System.out.println();
        System.out.println("  Extract data:");
        System.out.println("    java -jar SecTool.jar --extract -i <stego_image> -o <out_file>");
        System.out.println("    (Aliases: --image, --out)");
        System.out.println();
        System.out.println("  Analyze image capacity:");
        System.out.println("    java -jar SecTool.jar --info -i <image.png>");
        System.out.println();
        System.out.println("  Help:");
        System.out.println("    java -jar SecTool.jar --help");
    }
}
