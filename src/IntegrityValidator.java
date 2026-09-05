import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class IntegrityValidator {

    public static byte[] generateSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    public static boolean verify(byte[] originalHash, byte[] newHash) {
        if (originalHash == null || newHash == null) {
            return false;
        }
        if (originalHash.length != newHash.length) {
            return false;
        }
        return MessageDigest.isEqual(originalHash, newHash);
    }
}
