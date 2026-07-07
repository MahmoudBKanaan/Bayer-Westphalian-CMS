package com.bayerwestphalian.campaign.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.method.AdminOnly;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import com.bayerwestphalian.campaign.common.exception.ConflictException;
import com.bayerwestphalian.campaign.common.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserControllerTests {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000009901");
    private static final UUID ADMIN_ID = UUID.fromString("10000000-0000-0000-0000-000000009902");

    @Mock private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new UserController(userService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void controllerRequiresAdminMethodAuthorization() {
        assertThat(UserController.class.isAnnotationPresent(AdminOnly.class)).isTrue();
    }

    @Test
    void listsUsersWithOptionalStatusFilter() throws Exception {
        when(userService.listUsers(UserStatus.ACTIVE)).thenReturn(List.of(userView()));

        mockMvc.perform(get("/api/users").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Users loaded"))
                .andExpect(jsonPath("$.data[0].email").value("advisor@bayer-westphalian.test"))
                .andExpect(jsonPath("$.data[0].roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist());

        verify(userService).listUsers(UserStatus.ACTIVE);
    }

    @Test
    void getsUserById() throws Exception {
        when(userService.findById(USER_ID)).thenReturn(userView());

        mockMvc.perform(get("/api/users/{id}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User loaded"))
                .andExpect(jsonPath("$.data.id").value(USER_ID.toString()));

        verify(userService).findById(USER_ID);
    }

    @Test
    void createsUser() throws Exception {
        when(userService.createUser(any(CreateUserCommand.class))).thenReturn(userView());

        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "advisor@bayer-westphalian.test",
                                          "password": "StrongPassword!2026",
                                          "fullName": "Advisor User"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User created"))
                .andExpect(jsonPath("$.data.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.email").value("advisor@bayer-westphalian.test"))
                .andExpect(jsonPath("$.data.fullName").value("Advisor User"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        ArgumentCaptor<CreateUserCommand> commandCaptor =
                ArgumentCaptor.forClass(CreateUserCommand.class);
        verify(userService).createUser(commandCaptor.capture());
        assertThat(commandCaptor.getValue().email()).isEqualTo("advisor@bayer-westphalian.test");
        assertThat(commandCaptor.getValue().rawPassword()).isEqualTo("StrongPassword!2026");
        assertThat(commandCaptor.getValue().fullName()).isEqualTo("Advisor User");
    }

    @Test
    void rejectsInvalidCreateUserRequest() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/users"));
    }

    @Test
    void rejectsInvalidCreateUserEmail() throws Exception {
        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "not-an-email",
                                          "password": "StrongPassword!2026",
                                          "fullName": "Advisor User"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.validationErrors[0].field").value("email"));
    }

    @Test
    void mapsDuplicateCreateUserEmailToConflictResponse() throws Exception {
        when(userService.createUser(any(CreateUserCommand.class)))
                .thenThrow(new ConflictException("USER_EMAIL_EXISTS", "User email already exists"));

        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "email": "advisor@bayer-westphalian.test",
                                          "password": "StrongPassword!2026",
                                          "fullName": "Advisor User"
                                        }
                                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_EMAIL_EXISTS"))
                .andExpect(jsonPath("$.message").value("User email already exists"))
                .andExpect(jsonPath("$.path").value("/api/users"));
    }

    @Test
    void updatesUser() throws Exception {
        when(userService.updateUser(any(UUID.class), any(UpdateUserCommand.class)))
                .thenReturn(lockedUserView());

        mockMvc.perform(
                        put("/api/users/{id}", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "fullName": "Senior Advisor",
                                          "status": "LOCKED"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User updated"))
                .andExpect(jsonPath("$.data.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.fullName").value("Senior Advisor"))
                .andExpect(jsonPath("$.data.status").value("LOCKED"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        ArgumentCaptor<UpdateUserCommand> commandCaptor =
                ArgumentCaptor.forClass(UpdateUserCommand.class);
        verify(userService).updateUser(eq(USER_ID), commandCaptor.capture());
        assertThat(commandCaptor.getValue().fullName()).isEqualTo("Senior Advisor");
        assertThat(commandCaptor.getValue().status()).isEqualTo(UserStatus.LOCKED);
    }

    @Test
    void rejectsInvalidUpdateUserRequest() throws Exception {
        mockMvc.perform(
                        put("/api/users/{id}", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "fullName": " ",
                                          "status": "ACTIVE"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/users/" + USER_ID))
                .andExpect(jsonPath("$.validationErrors[0].field").value("fullName"));
    }

    @Test
    void mapsMissingUpdatedUserToNotFoundResponse() throws Exception {
        when(userService.updateUser(any(UUID.class), any(UpdateUserCommand.class)))
                .thenThrow(new ResourceNotFoundException("User", USER_ID));

        mockMvc.perform(
                        put("/api/users/{id}", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "fullName": "Senior Advisor",
                                          "status": "ACTIVE"
                                        }
                                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/users/" + USER_ID));
    }

    @Test
    void disablesUser() throws Exception {
        when(userService.disableUser(USER_ID)).thenReturn(disabledUserView());

        mockMvc.perform(patch("/api/users/{id}/disable", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User disabled"))
                .andExpect(jsonPath("$.data.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.email").value("advisor@bayer-westphalian.test"))
                .andExpect(jsonPath("$.data.fullName").value("Advisor User"))
                .andExpect(jsonPath("$.data.status").value("DISABLED"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        verify(userService).disableUser(USER_ID);
    }

    @Test
    void mapsMissingDisabledUserToNotFoundResponse() throws Exception {
        when(userService.disableUser(USER_ID))
                .thenThrow(new ResourceNotFoundException("User", USER_ID));

        mockMvc.perform(patch("/api/users/{id}/disable", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/users/" + USER_ID + "/disable"));

        verify(userService).disableUser(USER_ID);
    }

    @Test
    void resetsPassword() throws Exception {
        when(userService.resetPassword(USER_ID, "NewStrongPassword!2026")).thenReturn(userView());

        mockMvc.perform(
                        patch("/api/users/{id}/password", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "password": "NewStrongPassword!2026"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset"));

        verify(userService).resetPassword(USER_ID, "NewStrongPassword!2026");
    }

    @Test
    void assignsRole() throws Exception {
        when(userService.assignRole(USER_ID, SystemRoleName.CAMPAIGN_MANAGER, ADMIN_ID))
                .thenReturn(campaignManagerUserView());

        mockMvc.perform(
                        post("/api/users/{id}/roles", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "roleName": "CAMPAIGN_MANAGER",
                                          "assignedByUserId": "%s"
                                        }
                                        """
                                                .formatted(ADMIN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Role assigned"))
                .andExpect(jsonPath("$.data.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.roles[0]").value("CAMPAIGN_MANAGER"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        verify(userService).assignRole(USER_ID, SystemRoleName.CAMPAIGN_MANAGER, ADMIN_ID);
    }

    @Test
    void rejectsAssignRoleRequestWithoutRoleName() throws Exception {
        mockMvc.perform(
                        post("/api/users/{id}/roles", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "assignedByUserId": "%s"
                                        }
                                        """
                                                .formatted(ADMIN_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/users/" + USER_ID + "/roles"))
                .andExpect(jsonPath("$.validationErrors[0].field").value("roleName"));
    }

    @Test
    void mapsMissingAssignedRoleToNotFoundResponse() throws Exception {
        when(userService.assignRole(USER_ID, SystemRoleName.PRODUCT_MANAGER, ADMIN_ID))
                .thenThrow(new ResourceNotFoundException("Role", SystemRoleName.PRODUCT_MANAGER));

        mockMvc.perform(
                        post("/api/users/{id}/roles", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "roleName": "PRODUCT_MANAGER",
                                          "assignedByUserId": "%s"
                                        }
                                        """
                                                .formatted(ADMIN_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/users/" + USER_ID + "/roles"));

        verify(userService).assignRole(USER_ID, SystemRoleName.PRODUCT_MANAGER, ADMIN_ID);
    }

    @Test
    void mapsMissingUserToNotFoundResponse() throws Exception {
        when(userService.findById(USER_ID))
                .thenThrow(new ResourceNotFoundException("User", USER_ID));

        mockMvc.perform(get("/api/users/{id}", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private static UserView userView() {
        return new UserView(
                USER_ID,
                "advisor@bayer-westphalian.test",
                "Advisor User",
                UserStatus.ACTIVE,
                Instant.parse("2026-07-03T12:00:00Z"),
                List.of(SystemRoleName.ADMIN));
    }

    private static UserView lockedUserView() {
        return new UserView(
                USER_ID,
                "advisor@bayer-westphalian.test",
                "Senior Advisor",
                UserStatus.LOCKED,
                Instant.parse("2026-07-03T12:00:00Z"),
                List.of(SystemRoleName.ADMIN));
    }

    private static UserView disabledUserView() {
        return new UserView(
                USER_ID,
                "advisor@bayer-westphalian.test",
                "Advisor User",
                UserStatus.DISABLED,
                Instant.parse("2026-07-03T12:00:00Z"),
                List.of(SystemRoleName.ADMIN));
    }

    private static UserView campaignManagerUserView() {
        return new UserView(
                USER_ID,
                "advisor@bayer-westphalian.test",
                "Advisor User",
                UserStatus.ACTIVE,
                Instant.parse("2026-07-03T12:00:00Z"),
                List.of(SystemRoleName.CAMPAIGN_MANAGER));
    }
}
