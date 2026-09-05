import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.BadPaddingException;
import java.security.SecureRandom;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class CryptoService {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 16;

    public static byte[] encrypt(byte[] plaintext, char[] password) throws Exception {
        SecureRandom random = new SecureRandom();
        
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        
        SecretKey key = deriveKey(password, salt);
        
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        
        byte[] ciphertext = cipher.doFinal(plaintext);
        
        ByteBuffer buffer = ByteBuffer.allocate(SALT_LENGTH + IV_LENGTH + ciphertext.length);
        buffer.put(salt);
        buffer.put(iv);
        buffer.put(ciphertext);
        
        return buffer.array();
    }

    public static byte[] decrypt(byte[] payload, char[] password) throws InvalidPasswordException, Exception {
        if (payload.length < SALT_LENGTH + IV_LENGTH) {
            throw new Exception("Payload is too short to contain salt and IV.");
        }
        
        byte[] salt = Arrays.copyOfRange(payload, 0, SALT_LENGTH);
        byte[] iv = Arrays.copyOfRange(payload, SALT_LENGTH, SALT_LENGTH + IV_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(payload, SALT_LENGTH + IV_LENGTH, payload.length);
        
        SecretKey key = deriveKey(password, salt);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        
        try {
            return cipher.doFinal(ciphertext);
        } catch (BadPaddingException e) {
            throw new InvalidPasswordException("Decryption failed. Incorrect password or corrupted payload.", e);
        }
    }

    private static SecretKey deriveKey(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        // Clear password from PBEKeySpec
        spec.clearPassword();
        return new SecretKeySpec(keyBytes, "AES");
    }
}
