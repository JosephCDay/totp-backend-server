package com.josephcday.totp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang3.StringUtils;

/**
 * TOTP utilities for generating and validating one-time passwords. Compatible
 * with Google Authenticator and Authy.
 * 
 * @author Joseph Curtis Day
 */
public class TOTP {
    private static final int TOTP_INTERVAL = 30; // TOTP uses 30 second intervals
    private static final long ONE_SEC = 1000L;
    private static final int TOTP_DIGITS = 6; // Google Authenticator and Authy uses 6
    private static final int TOTP_SECRET_LENGTH = 16; // Google Authenticator and Authy uses 16
    private static final Base32 BASE32_CODEC = new Base32();
    public static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    public static final char[] BASE32_CHAR_ARRAY;

    static {
        BASE32_CHAR_ARRAY = BASE32_CHARS.toCharArray();
    }

    /**
     * Generates a random 16 character base32 string. Used as a stored shared
     * secret.
     * 
     * @see java.security.SecureRandom
     * @return base32 string
     */
    public static String b32Secret() {
        Random random = new SecureRandom();
        StringBuilder sb = new StringBuilder(TOTP_SECRET_LENGTH);
        for (int i = 0; i < TOTP_SECRET_LENGTH; i++) {
            sb.append(BASE32_CHAR_ARRAY[random.nextInt(32)]);
        }
        return sb.toString();
    }

    /**
     * Validates a token against a secret using provided timestamp. Checks in 30
     * second intervals. Checks interval using this instances currentTimeMillis().
     * 
     * @param b32Secret the TOTP stored shared secret
     * @param token     the one-time password token
     * @return true indicates the token passed for the provided time intervals.
     * @throws InvalidKeyException      exception
     * @throws NoSuchAlgorithmException exception
     */
    public static boolean validate(String b32Secret, int token) throws InvalidKeyException, NoSuchAlgorithmException {
        return validate(b32Secret, token, System.currentTimeMillis(), 0);
    }

    /**
     * Validates a token against a secret using provided timestamp. Checks in 30
     * second intervals. Checks interval using this instances currentTimeMillis().
     * Will check 'window' intervals before and after current interval for validity.
     * 
     * @param b32Secret the TOTP stored shared secret
     * @param token     the one-time password token
     * @param window    how many additional 30 second intervals to include before
     *                  and after
     * @return true indicates the token passed for the provided time intervals.
     * @throws InvalidKeyException      exception
     * @throws NoSuchAlgorithmException exception
     */
    public static boolean validate(String b32Secret, int token, int window)
            throws InvalidKeyException, NoSuchAlgorithmException {
        return validate(b32Secret, token, System.currentTimeMillis(), window);
    }

    /**
     * Validates a token against a secret using provided timestamp. Checks in 30
     * second intervals. Will check 'window' intervals before and after current
     * interval for validity.
     * 
     * @param b32Secret the TOTP stored shared secret
     * @param token     the one-time password token
     * @param timeUnix  the time in seconds to check
     * @param window    how many additional 30 second intervals to include before
     *                  and after
     * @return true indicates the token passed for the provided time intervals.
     * @throws InvalidKeyException      exception
     * @throws NoSuchAlgorithmException exception
     */
    public static boolean validate(String b32Secret, int token, int timeUnix, int window)
            throws InvalidKeyException, NoSuchAlgorithmException {
        return validate(b32Secret, token, timeUnix * ONE_SEC, window);
    }

    /**
     * Validates a token against a secret using provided timestamp. Checks in 30
     * second intervals. Will check 'window' intervals before and after current
     * interval for validity.
     * 
     * @param b32Secret  the TOTP stored shared secret
     * @param token      the one-time password token
     * @param timeMillis the time in milliseconds to check
     * @param window     how many additional 30 second intervals to include before
     *                   and after
     * @return true indicates the token passed for the provided time intervals.
     * @throws InvalidKeyException      exception
     * @throws NoSuchAlgorithmException exception
     */
    public static boolean validate(String b32Secret, int token, long timeMillis, int window)
            throws InvalidKeyException, NoSuchAlgorithmException {
        ArrayList<Long> intervals = new ArrayList<>();

        intervals.add(timeMillis);
        for (int i = 0; i < window; i++) {
            intervals.add(timeMillis + i * TOTP_INTERVAL * ONE_SEC);
            intervals.add(timeMillis - i * TOTP_INTERVAL * ONE_SEC);
        }

        Iterator<Long> iter = intervals.iterator();

        while (iter.hasNext()) {
            Long val = iter.next();
            int getToken = generateToken(b32Secret, val);
            if (getToken == token) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a token for the current 30 second interval. Uses this instances
     * currentTimeMillis().
     * 
     * @param b32Secret the TOTP stored shared secret
     * @return the one-time password token
     * @throws InvalidKeyException      exception
     * @throws NoSuchAlgorithmException exception
     */
    public static String getToken(String b32Secret) throws InvalidKeyException, NoSuchAlgorithmException {
        return getToken(b32Secret, System.currentTimeMillis());
    }

    /**
     * Gets a token for the current 30 second interval. Uses provided milliseconds,
     * and will round to nearest interval.
     * 
     * @param b32Secret the TOTP stored shared secret
     * @param timeUnix  the time in unixtime to generate
     * @return the one-time password token
     * @throws InvalidKeyException      exception
     * @throws NoSuchAlgorithmException exception
     */
    public static String getToken(String b32Secret, int timeUnix) throws InvalidKeyException, NoSuchAlgorithmException {
        return getToken(b32Secret, timeUnix * ONE_SEC);
    }

    /**
     * Gets a token for the current 30 second interval. Uses provided milliseconds,
     * and will round to nearest interval.
     * 
     * @param b32Secret  the TOTP stored shared secret
     * @param timeMillis the time in milliseconds to generate
     * @return the one-time password token
     * @throws InvalidKeyException      exception
     * @throws NoSuchAlgorithmException exception
     */
    public static String getToken(String b32Secret, long timeMillis)
            throws InvalidKeyException, NoSuchAlgorithmException {
        int number = generateToken(b32Secret, timeMillis);
        return StringUtils.leftPad(Integer.toString(number), TOTP_DIGITS, '0');
    }

    /**
     * Generates an integer token for the provided interval. Uses provided
     * milliseconds, and will round to nearest interval.
     * 
     * @param b32Secret  the TOTP stored shared secret
     * @param timeMillis the time in milliseconds to generate
     * @return the one-time password integer
     * @throws NoSuchAlgorithmException exception
     * @throws InvalidKeyException      exception
     */
    private static int generateToken(String b32Secret, long timeMillis)
            throws NoSuchAlgorithmException, InvalidKeyException {

        byte[] key = BASE32_CODEC.decode(b32Secret);

        ByteBuffer bb = ByteBuffer.allocate(8); // size of a long
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putLong(timeMillis / ONE_SEC / TOTP_INTERVAL);
        byte[] data = bb.array();

        SecretKeySpec sKey = new SecretKeySpec(key, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(sKey);
        byte[] hash = mac.doFinal(data);

        // get offset from hash
        int offset = hash[hash.length - 1] & 0xF;

        // get hash bytes as int from offset
        byte[] truncated = Arrays.copyOfRange(hash, offset, offset + 4);
        int retData = ByteBuffer.wrap(truncated).getInt();

        // drop top bit
        retData &= 0x7FFFFFFF;

        // keep only last 6 digits
        retData %= 1000000;
        return retData;
    }

    /**
     * Builds a URL used by most 2fa clients to register. Can be placed in a QR
     * scancode.
     * 
     * @param label  Common name of the registered 2fa secret
     * @param secret the TOTP stored shared secret
     * @return the usable URL
     */
    public static String generateUrl(String label, String secret) {
        StringBuilder sb = new StringBuilder(64);
        sb.append("otpauth://totp/").append(label).append("?secret=").append(secret);
        return sb.toString();
    }

}