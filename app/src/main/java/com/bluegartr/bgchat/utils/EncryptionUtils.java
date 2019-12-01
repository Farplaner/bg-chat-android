package com.bluegartr.bgchat.utils;

import android.util.Log;

import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Daniel on 8/2/14.
 */
public class EncryptionUtils {
    // Algorithm used
    private final static String ALGORITHM = "AES";

    /**
     * Encrypt data
     * @param secretKey - a secret key used for encryption
     * @param data - data to encrypt
     * @return Encrypted data
     * @throws Exception
     */
    public static String cipher(String secretKey, String data) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), secretKey.getBytes(), 128, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey key = new SecretKeySpec(tmp.getEncoded(), ALGORITHM);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            return toHex(cipher.doFinal(data.getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Decrypt data
     * @param secretKey - a secret key used for decryption
     * @param data - data to decrypt
     * @return Decrypted data
     * @throws Exception
     */
    public static String decipher(String secretKey, String data) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(secretKey.toCharArray(), secretKey.getBytes(), 128, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey key = new SecretKeySpec(tmp.getEncoded(), ALGORITHM);

            Cipher cipher = Cipher.getInstance(ALGORITHM);

            cipher.init(Cipher.DECRYPT_MODE, key);

            return new String(cipher.doFinal(toByte(data)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // Helper methods
    private static byte[] toByte(String hexString) {
        int len = hexString.length()/2;

        byte[] result = new byte[len];

        for (int i = 0; i < len; i++)
            result[i] = Integer.valueOf(hexString.substring(2*i, 2*i+2), 16).byteValue();
        return result;
    }

    public static String toHex(byte[] stringBytes) {
        StringBuffer result = new StringBuffer(2*stringBytes.length);

        for (int i = 0; i < stringBytes.length; i++) {
            result.append(HEX.charAt((stringBytes[i]>>4)&0x0f)).append(HEX.charAt(stringBytes[i]&0x0f));
        }

        return result.toString();
    }

    private final static String HEX = "0123456789ABCDEF";

    public static void main(String args[]) {
        String text = "This is a plain text";
        String key = "super-secret-key-0123123451";

        String crypted = null;
        String decrypted = null;

        try {
            crypted = cipher(key, text);
            decrypted = decipher(key, crypted);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // We have tested it in a Android activity code by displaying data :)
        Log.d("CRYPTO-TEST", "plain: " + text);
        Log.d("CRYPTO-TEST", "crypted: " + crypted);
        Log.d("CRYPTO-TEST", "decrypted: " + decrypted);
    }
}
