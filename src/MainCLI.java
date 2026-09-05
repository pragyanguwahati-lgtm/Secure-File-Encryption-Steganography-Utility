import java.awt.image.BufferedImage;
import java.io.Console;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MainCLI {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String mode = args[0];
        try {
            if ("--hide".equals(mode)) {
                handleHide(args);
            } else if ("--extract".equals(mode)) {
                handleExtract(args);
            } else {
                System.err.println("Unknown mode: " + mode);
                printUsage();
                System.exit(1);
            }
        } catch (InvalidPasswordException | TamperedFileException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void handleHide(String[] args) throws Exception {
        String targetPath = null;
        String coverPath = null;
        String outPath = null;

        for (int i = 1; i < args.length; i++) {
            if ("--target".equals(args[i]) && i + 1 < args.length) targetPath = args[++i];
            else if ("--cover".equals(args[i]) && i + 1 < args.length) coverPath = args[++i];
            else if ("--out".equals(args[i]) && i + 1 < args.length) outPath = args[++i];
        }

        if (targetPath == null || coverPath == null || outPath == null) {
            System.err.println("Missing arguments for hiding.");
            printUsage();
            System.exit(1);
        }

        System.out.println("Reading target file...");
        byte[] fileData = FileHandlerUtil.readFileToBytes(targetPath);
        
        System.out.println("Generating SHA-256 integrity hash...");
        byte[] hash = IntegrityValidator.generateSHA256(fileData);
        
        ByteBuffer plaintextBuffer = ByteBuffer.allocate(hash.length + fileData.length);
        plaintextBuffer.put(hash);
        plaintextBuffer.put(fileData);
        byte[] plaintext = plaintextBuffer.array();
        
        char[] password = readPassword("Enter password to encrypt: ");
        if (password.length == 0) {
            System.err.println("Password cannot be empty.");
            System.exit(1);
        }

        System.out.println("Encrypting data...");
        byte[] encryptedPayload = CryptoService.encrypt(plaintext, password);
        Arrays.fill(password, ' '); // Clear password from memory

        System.out.println("Reading cover image...");
        BufferedImage coverImage = FileHandlerUtil.readImage(coverPath);
        
        System.out.println("Embedding encrypted payload into image...");
        BufferedImage stegoImage = StegoService.embed(coverImage, encryptedPayload);
        
        System.out.println("Writing output image...");
        FileHandlerUtil.writeImage(stegoImage, "png", outPath);
        
        System.out.println("Success! Data hidden in " + outPath);
    }

    private static void handleExtract(String[] args) throws Exception {
        String imagePath = null;
        String outPath = null;

        for (int i = 1; i < args.length; i++) {
            if ("--image".equals(args[i]) && i + 1 < args.length) imagePath = args[++i];
            else if ("--out".equals(args[i]) && i + 1 < args.length) outPath = args[++i];
        }

        if (imagePath == null || outPath == null) {
            System.err.println("Missing arguments for extraction.");
            printUsage();
            System.exit(1);
        }

        System.out.println("Reading stego image...");
        BufferedImage stegoImage = FileHandlerUtil.readImage(imagePath);
        
        System.out.println("Extracting payload from image...");
        byte[] encryptedPayload = StegoService.extract(stegoImage);
        
        char[] password = readPassword("Enter password to decrypt: ");
        if (password.length == 0) {
            System.err.println("Password cannot be empty.");
            System.exit(1);
        }

        System.out.println("Decrypting data...");
        byte[] plaintext = CryptoService.decrypt(encryptedPayload, password);
        Arrays.fill(password, ' '); // Clear password from memory
        
        if (plaintext.length < 32) {
            throw new TamperedFileException("Decrypted data is too small to contain integrity hash.");
        }
        
        byte[] originalHash = Arrays.copyOfRange(plaintext, 0, 32);
        byte[] fileData = Arrays.copyOfRange(plaintext, 32, plaintext.length);
        
        System.out.println("Verifying data integrity...");
        byte[] newHash = IntegrityValidator.generateSHA256(fileData);
        if (!IntegrityValidator.verify(originalHash, newHash)) {
            throw new TamperedFileException("Integrity check failed. The file may have been tampered with or corrupted.");
        }
        
        System.out.println("Writing extracted file to disk...");
        FileHandlerUtil.writeBytesToFile(outPath, fileData);
        
        System.out.println("Success! Data extracted to " + outPath);
    }

    private static char[] readPassword(String prompt) {
        Console console = System.console();
        if (console == null) {
            System.out.print(prompt);
            return new java.util.Scanner(System.in).nextLine().toCharArray();
        }
        return console.readPassword(prompt);
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  To hide data:    java MainCLI --hide --target <secret_file> --cover <image.png> --out <hidden.png>");
        System.out.println("  To extract data: java MainCLI --extract --image <hidden.png> --out <output_file>");
    }
}
