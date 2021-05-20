package com.utils;

import java.util.Random;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Davide Chen
 *
 * Password encryption manager using SHA3-256
 */

public class PasswordManager {

    String ALGORITHM = "SHA3-256"; // algoritmo used
    // to generate random numbers - thread safe
    private static final Random RANDOM = new SecureRandom();
    private static final int SALT_SIZE = 64; // size in bytes of the salt


    /**
     * Generates salt of 64 bytes in Hex format
     *
     * @return character string containing the salt
     */
    public String getSalt() {
        byte[] salt = new byte[SALT_SIZE];
        RANDOM.nextBytes(salt);
        return bytesToHex(salt);
    }

    /**
     * Generate password hash through SHA3-256 algorithm
     *
     * @param password plaintext password
     * @param salt randomly generated salt
     * @return hash of the password, in base Hex
     */
    public String hash(String password, String salt) {
        String digest = "";
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance(ALGORITHM);
            final byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
            final byte[] saltBytes = salt.getBytes(StandardCharsets.UTF_8);
            final byte[] allToBeHashed = concat(passwordBytes, saltBytes);
            final byte[] hashBytes = messageDigest.digest(allToBeHashed);
            digest = bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException ignored) {} // impossibile avvenga
        return digest;
    }

    /**
     * Verify that the password matches
     *
     * @param password to verify
     * @param salt used to generate the password hash (persistent)
     * @param hash generated (persistent)
     * @return true if the password matches, false otherwise
     */
    public boolean isExpectedPassword(String password, String salt, String hash) {
        String generatedHash = hash(password, salt);
        return hash.equals(generatedHash);
    }

    /**
     * @param hash bytes to convert to Hex format
     *
     * @return Hex string corresponding to the input bytes
     * */
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * @param a array of bytes
     * @param b array of bytes
     *
     * @return concatenation of arrays a and b
     */
    public static byte[] concat (final byte[] a, final byte[] b) {
        byte[] c = new byte[a.length + b.length];
        int k = 0;
        for(byte x : a) {
            c[k++] = x;
        }
        for (byte x : b) {
            c[k++] = x;
        }
        return c;
    }
}
