package com.bayerwestphalian.campaign.user;

import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Email @NotBlank @Size(max = 255) @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @NotBlank @Size(max = 255) @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @NotBlank @Size(max = 255) @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "user_status")
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected User() {}

    private User(String email, String passwordHash, String fullName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
    }

    public static User create(String email, String passwordHash, String fullName) {
        return new User(email, passwordHash, fullName);
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public void rename(String fullName) {
        this.fullName = fullName;
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void recordLogin(Instant loginTime) {
        lastLoginAt = loginTime;
    }

    public void activate() {
        status = UserStatus.ACTIVE;
    }

    public void disable() {
        status = UserStatus.DISABLED;
    }

    public void lock() {
        status = UserStatus.LOCKED;
    }
}
