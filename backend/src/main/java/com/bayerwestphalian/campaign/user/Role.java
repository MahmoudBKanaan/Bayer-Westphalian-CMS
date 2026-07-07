package com.bayerwestphalian.campaign.user;

import com.bayerwestphalian.campaign.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    @NotNull @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "name", nullable = false, unique = true, columnDefinition = "system_role_name")
    private SystemRoleName name;

    @NotBlank @Size(max = 100) @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @NotBlank @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @NotBlank @Column(name = "allowed_functions", nullable = false, columnDefinition = "text")
    private String allowedFunctions;

    @Column(name = "mvp_role", nullable = false)
    private boolean mvpRole;

    protected Role() {}

    private Role(
            SystemRoleName name,
            String displayName,
            String description,
            String allowedFunctions,
            boolean mvpRole) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.allowedFunctions = allowedFunctions;
        this.mvpRole = mvpRole;
    }

    public static Role create(
            SystemRoleName name,
            String displayName,
            String description,
            String allowedFunctions,
            boolean mvpRole) {
        return new Role(name, displayName, description, allowedFunctions, mvpRole);
    }

    public SystemRoleName getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getAllowedFunctions() {
        return allowedFunctions;
    }

    public boolean isMvpRole() {
        return mvpRole;
    }

    public boolean isExtendedRole() {
        return !mvpRole;
    }

    public void updateMetadata(
            String displayName, String description, String allowedFunctions, boolean mvpRole) {
        this.displayName = displayName;
        this.description = description;
        this.allowedFunctions = allowedFunctions;
        this.mvpRole = mvpRole;
    }
}
