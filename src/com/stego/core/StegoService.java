package com.stego.core;

import com.stego.exception.CapacityExceededException;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class StegoService {

    // 4-byte magic signature to identify valid steganography payloads: 'S', 'T', 'E', 'G'
    public static final byte[] MAGIC_HEADER = new byte[] { 'S', 'T', 'E', 'G' };
    public static final int HEADER_LENGTH = 8; // 4 bytes magic + 4 bytes payload length

    /**
     * Calculates the maximum payload bytes that can be embedded into the image.
     */
    public static long getPayloadCapacityBytes(BufferedImage image) {
        long totalBits = (long) image.getWidth() * image.getHeight() * 3L;
        long totalBytes = totalBits / 8L;
        return Math.max(0, totalBytes - HEADER_LENGTH);
    }

    /**
     * Checks if the image contains the 'STEG' magic header.
     */
    public static boolean hasValidSignature(BufferedImage image) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            long maxCapacityBits = (long) width * height * 3L;
            if (maxCapacityBits < 32) {
                return false;
            }

            byte[] magic = new byte[4];
            int bitIndex = 0;
            int byteIndex = 0;

            for (int y = 0; y < height && byteIndex < 4; y++) {
                for (int x = 0; x < width && byteIndex < 4; x++) {
                    int pixel = image.getRGB(x, y);
                    int[] channels = new int[] { (pixel >> 16) & 0xFF, (pixel >> 8) & 0xFF, pixel & 0xFF };

                    for (int c : channels) {
                        if (byteIndex < 4) {
                            int bit = c & 1;
                            magic[byteIndex] = (byte) ((magic[byteIndex] << 1) | bit);
                            bitIndex++;
                            if (bitIndex == 8) {
                                bitIndex = 0;
                                byteIndex++;
                            }
                        }
                    }
                }
            }
            return Arrays.equals(magic, MAGIC_HEADER);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Embeds payload into the RGB channels of the image using LSB steganography.
     */
    public static BufferedImage embed(BufferedImage image, byte[] payload) throws Exception {
        int width = image.getWidth();
        int height = image.getHeight();

        int totalBytes = HEADER_LENGTH + payload.length;
        long totalBits = (long) totalBytes * 8L;
        long maxCapacityBits = (long) width * height * 3L;

        if (totalBits > maxCapacityBits) {
            long maxAllowed = (maxCapacityBits / 8L) - HEADER_LENGTH;
            throw new CapacityExceededException("Cover image is too small. Required capacity: " 
                    + payload.length + " bytes, Available capacity: " + maxAllowed + " bytes.");
        }

        // Prepare embedding buffer: [4 bytes MAGIC] + [4 bytes Length] + [Payload]
        byte[] dataToEmbed = new byte[totalBytes];
        ByteBuffer buffer = ByteBuffer.wrap(dataToEmbed);
        buffer.put(MAGIC_HEADER);
        buffer.putInt(payload.length);
        buffer.put(payload);

        int bitIndex = 0;
        int dataIndex = 0;

        for (int y = 0; y < height && dataIndex < totalBytes; y++) {
            for (int x = 0; x < width && dataIndex < totalBytes; x++) {
                int pixel = image.getRGB(x, y);

                int a = (pixel >> 24) & 0xFF;
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;

                // Embed into Red LSB
                if (dataIndex < totalBytes) {
                    int bit = getBit(dataToEmbed[dataIndex], 7 - bitIndex);
                    r = (r & 0xFE) | bit;
                    bitIndex++;
                    if (bitIndex == 8) { bitIndex = 0; dataIndex++; }
                }

                // Embed into Green LSB
                if (dataIndex < totalBytes) {
                    int bit = getBit(dataToEmbed[dataIndex], 7 - bitIndex);
                    g = (g & 0xFE) | bit;
                    bitIndex++;
                    if (bitIndex == 8) { bitIndex = 0; dataIndex++; }
                }

                // Embed into Blue LSB
                if (dataIndex < totalBytes) {
                    int bit = getBit(dataToEmbed[dataIndex], 7 - bitIndex);
                    b = (b & 0xFE) | bit;
                    bitIndex++;
                    if (bitIndex == 8) { bitIndex = 0; dataIndex++; }
                }

                int newPixel = (a << 24) | (r << 16) | (g << 8) | b;
                image.setRGB(x, y, newPixel);
            }
        }

        return image;
    }

    /**
     * Extracts payload from the RGB channels of the image.
     */
    public static byte[] extract(BufferedImage image) throws Exception {
        int width = image.getWidth();
        int height = image.getHeight();
        long maxCapacityBits = (long) width * height * 3L;

        if (maxCapacityBits < (long) HEADER_LENGTH * 8L) {
            throw new Exception("Image is too small to contain a valid steganography header.");
        }

        byte[] headerBytes = new byte[HEADER_LENGTH];
        int headerBitIndex = 0;
        int headerByteIndex = 0;

        int curX = 0;
        int curY = 0;
        int curChannel = 0; // 0=R, 1=G, 2=B

        // Extract header
        outerHeader:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                int[] channels = new int[] { (pixel >> 16) & 0xFF, (pixel >> 8) & 0xFF, pixel & 0xFF };

                for (int c = 0; c < 3; c++) {
                    int bit = channels[c] & 1;
                    headerBytes[headerByteIndex] = (byte) ((headerBytes[headerByteIndex] << 1) | bit);
                    headerBitIndex++;
                    if (headerBitIndex == 8) {
                        headerBitIndex = 0;
                        headerByteIndex++;
                        if (headerByteIndex == HEADER_LENGTH) {
                            curX = x;
                            curY = y;
                            curChannel = c + 1;
                            if (curChannel == 3) {
                                curChannel = 0;
                                curX++;
                                if (curX == width) {
                                    curX = 0;
                                    curY++;
                                }
                            }
                            break outerHeader;
                        }
                    }
                }
            }
        }

        // Validate Magic Header
        byte[] extractedMagic = Arrays.copyOfRange(headerBytes, 0, 4);
        if (!Arrays.equals(extractedMagic, MAGIC_HEADER)) {
            throw new Exception("Image does not contain a valid steganography payload (magic header mismatch).");
        }

        // Read payload length
        ByteBuffer lengthBuffer = ByteBuffer.wrap(headerBytes, 4, 4);
        int payloadLength = lengthBuffer.getInt();

        if (payloadLength <= 0 || ((long) HEADER_LENGTH + payloadLength) * 8L > maxCapacityBits) {
            throw new Exception("Extracted payload length (" + payloadLength + " bytes) is invalid or exceeds image capacity.");
        }

        // Extract payload
        byte[] payload = new byte[payloadLength];
        int payloadByteIndex = 0;
        int payloadBitIndex = 0;

        for (int y = curY; y < height && payloadByteIndex < payloadLength; y++) {
            int startX = (y == curY) ? curX : 0;
            for (int x = startX; x < width && payloadByteIndex < payloadLength; x++) {
                int pixel = image.getRGB(x, y);
                int[] channels = new int[] { (pixel >> 16) & 0xFF, (pixel >> 8) & 0xFF, pixel & 0xFF };

                int startC = (y == curY && x == curX) ? curChannel : 0;
                for (int c = startC; c < 3 && payloadByteIndex < payloadLength; c++) {
                    int bit = channels[c] & 1;
                    payload[payloadByteIndex] = (byte) ((payload[payloadByteIndex] << 1) | bit);
                    payloadBitIndex++;
                    if (payloadBitIndex == 8) {
                        payloadBitIndex = 0;
                        payloadByteIndex++;
                    }
                }
            }
        }

        return payload;
    }

    private static int getBit(byte b, int position) {
        return (b >> position) & 1;
    }
}
