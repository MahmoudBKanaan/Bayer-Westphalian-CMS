package com.bayerwestphalian.campaign.settings;

import com.bayerwestphalian.campaign.auth.method.AdminOnly;
import com.bayerwestphalian.campaign.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST boundary for the System Settings screen (KB item 534).
 *
 * <p>Admin-only read/update of business limits (monthly contact, send retry, uninterested
 * exclusion days). Writes are not available to non-admin roles.
 */
@AdminOnly
@RestController
@RequestMapping("/api/system-settings")
public class SystemSettingsController {

    private final SystemSettingsService systemSettingsService;

    public SystemSettingsController(SystemSettingsService systemSettingsService) {
        this.systemSettingsService = systemSettingsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<SystemSettingsView>> getSettings() {
        SystemSettingsView settings = systemSettingsService.getSettings();
        return ResponseEntity.ok(ApiResponse.success("System settings loaded", settings));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<SystemSettingsView>> updateSettings(
            @Valid @RequestBody UpdateSystemSettingsRequest request) {
        SystemSettingsView settings = systemSettingsService.updateSettings(request.toCommand());
        return ResponseEntity.ok(ApiResponse.success("System settings updated", settings));
    }
}
