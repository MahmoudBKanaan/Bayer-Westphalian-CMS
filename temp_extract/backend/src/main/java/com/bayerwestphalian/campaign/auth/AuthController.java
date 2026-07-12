package com.bayerwestphalian.campaign.auth;

import com.bayerwestphalian.campaign.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthenticatedSession>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthenticatedSession session =
                authService.loginSession(request.email(), request.password());

        return ResponseEntity.ok(ApiResponse.success("Login successful", session));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthenticatedSession>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthenticatedSession session = authService.refreshSession(request.refreshToken());

        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", session));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        authService.logoutSession(authorizationHeader);

        return ResponseEntity.ok(ApiResponse.<Void>success("Logout successful", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthenticatedUser>> me(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        AuthenticatedUser user = authService.getCurrentSessionUser(authorizationHeader);

        return ResponseEntity.ok(ApiResponse.success("Current user loaded", user));
    }
}
