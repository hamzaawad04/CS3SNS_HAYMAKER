package server;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;


public class PasswordHasher {

    static final int ITERATIONS = 100000;
    static final int KEY_BITS = 256;

    static byte[] derive(char[] password, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return f.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }
    
    public static String hashPassword(char[] plainPassword) throws Exception{
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] dk = derive(plainPassword, salt, ITERATIONS);
        Base64.Encoder b64 = Base64.getEncoder().withoutPadding();
        return ITERATIONS + ":" + b64.encodeToString(salt) + ":" + b64.encodeToString(dk);
    }

    public static boolean verify(char[] plainPassword, String stored) throws Exception {
        String[] p = stored.split(":");
        int iterations = Integer.parseInt(p[0]);
        byte[] salt = Base64.getDecoder().decode(p[1]);

        byte[] expected = Base64.getDecoder().decode(p[2]);
        byte[] actual = derive(plainPassword, salt, iterations);

        return MessageDigest.isEqual(expected, actual);
    }
}

