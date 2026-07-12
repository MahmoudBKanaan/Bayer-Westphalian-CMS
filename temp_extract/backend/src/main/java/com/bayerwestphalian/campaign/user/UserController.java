package com.bayerwestphalian.campaign.user;

import com.bayerwestphalian.campaign.auth.method.AdminOnly;
import com.bayerwestphalian.campaign.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AdminOnly
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserView>>> listUsers(
            @RequestParam(required = false) UserStatus status) {
        List<UserView> users = userService.listUsers(status);

        return ResponseEntity.ok(ApiResponse.success("Users loaded", users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserView>> getUser(@PathVariable UUID id) {
        UserView user = userService.findById(id);

        return ResponseEntity.ok(ApiResponse.success("User loaded", user));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserView>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserView user = userService.createUser(request.toCommand());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created", user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserView>> updateUser(
            @PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        UserView user = userService.updateUser(id, request.toCommand());

        return ResponseEntity.ok(ApiResponse.success("User updated", user));
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<UserView>> disableUser(@PathVariable UUID id) {
        UserView user = userService.disableUser(id);

        return ResponseEntity.ok(ApiResponse.success("User disabled", user));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<ApiResponse<UserView>> resetPassword(
            @PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        UserView user = userService.resetPassword(id, request.password());

        return ResponseEntity.ok(ApiResponse.success("Password reset", user));
    }

    @PostMapping("/{id}/roles")
    public ResponseEntity<ApiResponse<UserView>> assignRole(
            @PathVariable UUID id, @Valid @RequestBody AssignRoleRequest request) {
        UserView user = userService.assignRole(id, request.roleName(), request.assignedByUserId());

        return ResponseEntity.ok(ApiResponse.success("Role assigned", user));
    }
}
