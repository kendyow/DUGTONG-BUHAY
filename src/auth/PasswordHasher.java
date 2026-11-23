package auth;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HexFormat;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Creates and verifies password records. Callers should clear their password arrays after use. */
public final class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final String RECORD_PREFIX = "pbkdf2-sha256$" + ITERATIONS + "$";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private PasswordHasher() {
    }

    /** Returns a record containing the algorithm, iteration count, random salt, and derived hash. */
    public static String hash(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password must not be empty.");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(password, salt);
        try {
            return RECORD_PREFIX + HEX.formatHex(salt) + "$" + HEX.formatHex(derived);
        } finally {
            Arrays.fill(derived, (byte) 0);
        }
    }

    /** Returns false for a wrong password or an invalid or unsupported record. */
    public static boolean verify(char[] password, String record) {
        if (password == null || password.length == 0 || record == null
                || !record.startsWith(RECORD_PREFIX)) {
            return false;
        }

        int saltStart = RECORD_PREFIX.length();
        int separator = saltStart + SALT_BYTES * 2;
        int hashStart = separator + 1;
        if (record.length() != hashStart + HASH_BYTES * 2 || record.charAt(separator) != '$') {
            return false;
        }

        byte[] salt;
        byte[] expected;
        try {
            salt = HEX.parseHex(record, saltStart, separator);
            expected = HEX.parseHex(record, hashStart, record.length());
        } catch (IllegalArgumentException e) {
            return false;
        }

        try {
            byte[] actual = derive(password, salt);
            try {
                return MessageDigest.isEqual(expected, actual);
            } finally {
                Arrays.fill(actual, (byte) 0);
            }
        } finally {
            Arrays.fill(expected, (byte) 0);
        }
    }

    private static byte[] derive(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, HASH_BYTES * Byte.SIZE);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is unavailable.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
