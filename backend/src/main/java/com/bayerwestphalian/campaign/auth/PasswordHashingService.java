package com.bayerwestphalian.campaign.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PasswordHashingService {

    private final PasswordEncoder passwordEncoder;

    public PasswordHashingService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String hash(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || !StringUtils.hasText(passwordHash)) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, passwordHash);
    }

    public boolean needsRehash(String passwordHash) {
        return !StringUtils.hasText(passwordHash) || passwordEncoder.upgradeEncoding(passwordHash);
    }
}
