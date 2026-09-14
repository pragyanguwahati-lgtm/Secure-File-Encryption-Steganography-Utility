package com.stego.test;

import com.stego.core.StegoService;
import com.stego.crypto.CryptoService;
import com.stego.crypto.IntegrityValidator;
import com.stego.exception.CapacityExceededException;
import com.stego.exception.InvalidPasswordException;
import com.stego.exception.TamperedFileException;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TestRunner {

    private static int testsPassed = 0;
    private static int totalTests = 0;

    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println("           SECTOOL AUTOMATED VALIDATION TEST SUITE           ");
        System.out.println("=============================================================");

        runTest("Roundtrip Encryption & Steganography", TestRunner::testRoundtrip);
        runTest("Invalid Password Rejection", TestRunner::testInvalidPassword);
        runTest("Bit Tampering & Integrity Verification", TestRunner::testTamperingDetection);
        runTest("Capacity Limit Bounds Check", TestRunner::testCapacityLimit);
        runTest("Non-Stego Clean Image Header Rejection", TestRunner::testNonStegoImageRejection);

        System.out.println("-------------------------------------------------------------");
        System.out.printf("Summary: %d / %d Tests Passed (%.1f%% Success Rate)%n",
                testsPassed, totalTests, (testsPassed * 100.0 / totalTests));
        System.out.println("=============================================================");

        if (testsPassed != totalTests) {
            System.exit(1);
        }
    }

    private static void runTest(String testName, TestCase testCase) {
        totalTests++;
        try {
            testCase.execute();
            testsPassed++;
            System.out.printf("[PASS] %-45s : PASSED%n", testName);
        } catch (Throwable t) {
            System.out.printf("[FAIL] %-45s : FAILED (%s)%n", testName, t.getMessage());
        }
    }

    @FunctionalInterface
    interface TestCase {
        void execute() throws Exception;
    }

    private static BufferedImage createSampleImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(new Color(120, 180, 240));
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();
        return image;
    }

    private static void testRoundtrip() throws Exception {
        BufferedImage cover = createSampleImage(100, 100);
        byte[] originalData = "Secret confidential data package CSE2006-VIT".getBytes(StandardCharsets.UTF_8);
        char[] password = "MasterKey@2026".toCharArray();

        // 1. Generate SHA-256
        byte[] hash = IntegrityValidator.generateSHA256(originalData);

        // 2. Prepend hash + plaintext
        ByteBuffer buf = ByteBuffer.allocate(hash.length + originalData.length);
        buf.put(hash);
        buf.put(originalData);
        byte[] plaintext = buf.array();

        // 3. Encrypt
        byte[] encryptedPayload = CryptoService.encrypt(plaintext, password);

        // 4. Embed into image
        BufferedImage stego = StegoService.embed(cover, encryptedPayload);

        // 5. Extract
        byte[] extractedPayload = StegoService.extract(stego);

        // 6. Decrypt
        byte[] decryptedPlaintext = CryptoService.decrypt(extractedPayload, password);

        // 7. Verify integrity
        byte[] extractedHash = Arrays.copyOfRange(decryptedPlaintext, 0, 32);
        byte[] extractedData = Arrays.copyOfRange(decryptedPlaintext, 32, decryptedPlaintext.length);

        if (!IntegrityValidator.verify(extractedHash, extractedData)) {
            throw new Exception("Integrity hash comparison failed during roundtrip.");
        }
        if (!Arrays.equals(originalData, extractedData)) {
            throw new Exception("Extracted data does not match original data.");
        }
    }

    private static void testInvalidPassword() throws Exception {
        BufferedImage cover = createSampleImage(100, 100);
        byte[] originalData = "Confidential payload".getBytes(StandardCharsets.UTF_8);
        char[] correctPassword = "CorrectPassword123".toCharArray();
        char[] wrongPassword = "WrongPassword999".toCharArray();

        byte[] hash = IntegrityValidator.generateSHA256(originalData);
        ByteBuffer buf = ByteBuffer.allocate(hash.length + originalData.length);
        buf.put(hash);
        buf.put(originalData);

        byte[] encrypted = CryptoService.encrypt(buf.array(), correctPassword);
        BufferedImage stego = StegoService.embed(cover, encrypted);
        byte[] extracted = StegoService.extract(stego);

        try {
            CryptoService.decrypt(extracted, wrongPassword);
            throw new Exception("Decryption succeeded with invalid password when it should have failed!");
        } catch (InvalidPasswordException e) {
            // Expected
        }
    }

    private static void testTamperingDetection() throws Exception {
        BufferedImage cover = createSampleImage(100, 100);
        byte[] originalData = "Tamper proof test message".getBytes(StandardCharsets.UTF_8);
        char[] password = "SecureKey123".toCharArray();

        byte[] hash = IntegrityValidator.generateSHA256(originalData);
        ByteBuffer buf = ByteBuffer.allocate(hash.length + originalData.length);
        buf.put(hash);
        buf.put(originalData);

        byte[] encrypted = CryptoService.encrypt(buf.array(), password);
        BufferedImage stego = StegoService.embed(cover, encrypted);

        // Tamper with a pixel in the payload region (pixel 25 is within the ciphertext region)
        int pixel = stego.getRGB(25, 0);
        stego.setRGB(25, 0, pixel ^ 0x01); // flip 1 bit

        byte[] extracted = StegoService.extract(stego);

        try {
            byte[] decrypted = CryptoService.decrypt(extracted, password);
            byte[] extractedHash = Arrays.copyOfRange(decrypted, 0, 32);
            byte[] extractedData = Arrays.copyOfRange(decrypted, 32, decrypted.length);

            if (!IntegrityValidator.verify(extractedHash, extractedData)) {
                // Correctly caught by hash validation
                return;
            }
            throw new Exception("Tampered data was not detected by integrity check!");
        } catch (InvalidPasswordException | TamperedFileException e) {
            // Expected: CBC padding error or integrity hash mismatch
        }
    }

    private static void testCapacityLimit() throws Exception {
        // Very small image: 5x5 pixels = 25 pixels * 3 channels = 75 bits = 9 bytes capacity
        BufferedImage smallCover = createSampleImage(5, 5);
        byte[] hugePayload = new byte[500]; // 500 bytes will not fit in 9 bytes

        try {
            StegoService.embed(smallCover, hugePayload);
            throw new Exception("Capacity check failed to reject oversized payload!");
        } catch (CapacityExceededException e) {
            // Expected
        }
    }

    private static void testNonStegoImageRejection() throws Exception {
        BufferedImage cleanImage = createSampleImage(50, 50);
        try {
            StegoService.extract(cleanImage);
            throw new Exception("Extraction should fail on clean image without STEG header!");
        } catch (Exception e) {
            if (!e.getMessage().contains("magic header") && !e.getMessage().contains("valid steganography payload")) {
                throw new Exception("Unexpected error message: " + e.getMessage());
            }
        }
    }
}
