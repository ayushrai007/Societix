package com.societix.platform.identity.domain;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Encrypts verified E.164 phone numbers before they are persisted. */
@Component
public class PhoneNumberProtector {

    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final byte[] ASSOCIATED_DATA = "societix.identity.phone.v1".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    private static final Pattern INDIAN_E164 = Pattern.compile("\\+91[6-9][0-9]{9}");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final byte[] key;

    public PhoneNumberProtector(@Value("${societix.security.phone-encryption-key:}") String encodedKey) {
        this.key = decodeKey(encodedKey);
    }

    public byte[] encrypt(String phoneNumber) {
        if (phoneNumber == null || !INDIAN_E164.matcher(phoneNumber).matches()) {
            throw new IllegalArgumentException("A verified Indian phone number in E.164 format is required");
        }
        requireConfiguredKey();
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            SECURE_RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(ASSOCIATED_DATA);
            byte[] ciphertext = cipher.doFinal(phoneNumber.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return ByteBuffer.allocate(nonce.length + ciphertext.length).put(nonce).put(ciphertext).array();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not protect phone contact data", exception);
        }
    }

    public boolean isConfigured() {
        return key != null;
    }

    public boolean supportsPhoneNumber(String phoneNumber) {
        return phoneNumber != null && INDIAN_E164.matcher(phoneNumber).matches();
    }

    public String decrypt(byte[] encryptedPhone) {
        requireConfiguredKey();
        if (encryptedPhone == null || encryptedPhone.length <= NONCE_LENGTH) {
            throw new IllegalArgumentException("Encrypted phone contact data is invalid");
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(encryptedPhone);
            byte[] nonce = new byte[NONCE_LENGTH];
            buffer.get(nonce);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(ASSOCIATED_DATA);
            return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not read protected phone contact data", exception);
        }
    }

    private byte[] decodeKey(String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encodedKey);
            if (decoded.length != 32) {
                throw new IllegalArgumentException("Phone encryption key must decode to exactly 32 bytes");
            }
            return decoded;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("SOCIETIX_PHONE_ENCRYPTION_KEY must be a base64-encoded 32-byte key", exception);
        }
    }

    private void requireConfiguredKey() {
        if (key == null) {
            throw new IllegalStateException("Phone contact encryption is not configured");
        }
    }
}
