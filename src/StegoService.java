import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class StegoService {

    public static BufferedImage embed(BufferedImage image, byte[] payload) throws Exception {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Data to embed: 4 bytes length + payload
        int totalBytes = 4 + payload.length;
        long totalBits = (long) totalBytes * 8;
        
        long maxCapacityBits = (long) width * height * 3;
        if (totalBits > maxCapacityBits) {
            throw new Exception("Cover image is too small to hold the payload. Required bits: " + totalBits + ", Capacity: " + maxCapacityBits);
        }
        
        byte[] dataToEmbed = new byte[totalBytes];
        ByteBuffer.wrap(dataToEmbed).putInt(payload.length).put(payload);
        
        int bitIndex = 0;
        int dataIndex = 0;
        
        for (int y = 0; y < height && dataIndex < totalBytes; y++) {
            for (int x = 0; x < width && dataIndex < totalBytes; x++) {
                int pixel = image.getRGB(x, y);
                
                int a = (pixel >> 24) & 0xFF;
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                
                // Embed in R
                if (dataIndex < totalBytes) {
                    int bit = getBit(dataToEmbed[dataIndex], 7 - bitIndex);
                    r = (r & 0xFE) | bit;
                    bitIndex++;
                    if (bitIndex == 8) { bitIndex = 0; dataIndex++; }
                }
                
                // Embed in G
                if (dataIndex < totalBytes) {
                    int bit = getBit(dataToEmbed[dataIndex], 7 - bitIndex);
                    g = (g & 0xFE) | bit;
                    bitIndex++;
                    if (bitIndex == 8) { bitIndex = 0; dataIndex++; }
                }
                
                // Embed in B
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

    public static byte[] extract(BufferedImage image) throws Exception {
        int width = image.getWidth();
        int height = image.getHeight();
        
        int payloadLength = 0;
        byte[] payload = null;
        
        int bitIndex = 0;
        int dataIndex = 0;
        int currentByte = 0;
        
        // State 0: extracting length (4 bytes)
        // State 1: extracting payload
        int state = 0;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);
                
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                
                // Extract from R
                currentByte = (currentByte << 1) | (r & 1);
                bitIndex++;
                if (bitIndex == 8) {
                    if (state == 0) {
                        payloadLength = (payloadLength << 8) | currentByte;
                        dataIndex++;
                        if (dataIndex == 4) {
                            if (payloadLength < 0 || payloadLength > (width * height * 3 / 8) - 4) {
                                throw new Exception("Invalid payload length extracted. Image might not contain valid steganography data.");
                            }
                            payload = new byte[payloadLength];
                            state = 1;
                            dataIndex = 0;
                        }
                    } else if (state == 1) {
                        payload[dataIndex++] = (byte) currentByte;
                        if (dataIndex == payloadLength) {
                            return payload;
                        }
                    }
                    bitIndex = 0;
                    currentByte = 0;
                }
                
                // Extract from G
                currentByte = (currentByte << 1) | (g & 1);
                bitIndex++;
                if (bitIndex == 8) {
                    if (state == 0) {
                        payloadLength = (payloadLength << 8) | currentByte;
                        dataIndex++;
                        if (dataIndex == 4) {
                            if (payloadLength < 0 || payloadLength > (width * height * 3 / 8) - 4) {
                                throw new Exception("Invalid payload length extracted. Image might not contain valid steganography data.");
                            }
                            payload = new byte[payloadLength];
                            state = 1;
                            dataIndex = 0;
                        }
                    } else if (state == 1) {
                        payload[dataIndex++] = (byte) currentByte;
                        if (dataIndex == payloadLength) {
                            return payload;
                        }
                    }
                    bitIndex = 0;
                    currentByte = 0;
                }
                
                // Extract from B
                currentByte = (currentByte << 1) | (b & 1);
                bitIndex++;
                if (bitIndex == 8) {
                    if (state == 0) {
                        payloadLength = (payloadLength << 8) | currentByte;
                        dataIndex++;
                        if (dataIndex == 4) {
                            if (payloadLength < 0 || payloadLength > (width * height * 3 / 8) - 4) {
                                throw new Exception("Invalid payload length extracted. Image might not contain valid steganography data.");
                            }
                            payload = new byte[payloadLength];
                            state = 1;
                            dataIndex = 0;
                        }
                    } else if (state == 1) {
                        payload[dataIndex++] = (byte) currentByte;
                        if (dataIndex == payloadLength) {
                            return payload;
                        }
                    }
                    bitIndex = 0;
                    currentByte = 0;
                }
            }
        }
        
        throw new Exception("Finished scanning image but could not extract full payload. Data might be corrupted.");
    }
    
    private static int getBit(byte b, int position) {
        return (b >> position) & 1;
    }
}
