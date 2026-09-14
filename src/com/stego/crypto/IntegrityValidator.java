package com.stego.crypto;

import java.security.MessageDigest;

public class IntegrityValidator {

    private static final String HASH_ALGORITHM = "SHA-256";

    public static byte[] generateSHA256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        return digest.digest(data);
    }

    public static boolean verify(byte[] expectedHash, byte[] actualData) throws Exception {
        byte[] actualHash = generateSHA256(actualData);
        return MessageDigest.isEqual(expectedHash, actualHash);
    }
}
