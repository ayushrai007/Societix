package com.societix.platform.identity.internal;

import com.societix.platform.identity.domain.PhoneNumberProtector;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Owns Societix's encrypted copy of a Firebase-verified contact phone number. */
@Service
public class IdentityContactService {

    private final JdbcTemplate jdbcTemplate;
    private final PhoneNumberProtector phoneNumberProtector;

    public IdentityContactService(JdbcTemplate jdbcTemplate, PhoneNumberProtector phoneNumberProtector) {
        this.jdbcTemplate = jdbcTemplate;
        this.phoneNumberProtector = phoneNumberProtector;
    }

    @Transactional
    public void updateVerifiedPhone(String firebaseUid, String verifiedPhoneNumber) {
        if (!phoneNumberProtector.isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Phone contact encryption is not configured");
        }
        int updated = jdbcTemplate.update(
                "UPDATE identity_account SET phone_number_cipher = ? WHERE firebase_uid = ?",
                phoneNumberProtector.encrypt(verifiedPhoneNumber),
                firebaseUid);
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Societix account was not found");
        }
    }
}
