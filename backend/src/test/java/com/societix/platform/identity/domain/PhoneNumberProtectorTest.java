package com.societix.platform.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PhoneNumberProtectorTest {

    private static final String TEST_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    private final PhoneNumberProtector protector = new PhoneNumberProtector(TEST_KEY);

    @Test
    void encryptsAndDecryptsVerifiedIndianPhoneNumber() {
        String phoneNumber = "+919876543210";

        byte[] protectedPhone = protector.encrypt(phoneNumber);

        assertThat(protectedPhone).hasSizeGreaterThan(phoneNumber.length());
        assertThat(protector.decrypt(protectedPhone)).isEqualTo(phoneNumber);
    }

    @Test
    void rejectsUnverifiedOrNonIndianPhoneNumber() {
        assertThatThrownBy(() -> protector.encrypt("9876543210"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("E.164");
    }

    @Test
    void wrongEncryptionKeyCannotDecryptContact() {
        byte[] encrypted = protector.encrypt("+919876543210");
        PhoneNumberProtector otherKey = new PhoneNumberProtector("AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");

        assertThatThrownBy(() -> otherKey.decrypt(encrypted))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not read protected phone contact data");
    }
}
