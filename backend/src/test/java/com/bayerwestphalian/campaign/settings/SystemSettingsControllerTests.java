package com.bayerwestphalian.campaign.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bayerwestphalian.campaign.auth.method.AdminOnly;
import com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemSettingsController (item 534)")
class SystemSettingsControllerTests {

    @Mock private SystemSettingsService systemSettingsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new SystemSettingsController(systemSettingsService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void controllerRequiresAdminMethodAuthorization() {
        assertThat(SystemSettingsController.class.isAnnotationPresent(AdminOnly.class)).isTrue();
    }

    @Test
    void getsSystemSettings() throws Exception {
        when(systemSettingsService.getSettings()).thenReturn(sampleView());

        mockMvc.perform(get("/api/system-settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("System settings loaded"))
                .andExpect(jsonPath("$.data.monthlyContactLimit").value(3))
                .andExpect(jsonPath("$.data.sendRetryLimit").value(3))
                .andExpect(jsonPath("$.data.uninterestedExclusionDays").value(90));

        verify(systemSettingsService).getSettings();
    }

    @Test
    void updatesSystemSettings() throws Exception {
        when(systemSettingsService.updateSettings(any(UpdateSystemSettingsCommand.class)))
                .thenReturn(
                        new SystemSettingsView(
                                SystemSettings.SINGLETON_ID,
                                5,
                                4,
                                120,
                                UUID.fromString("10000000-0000-0000-0000-000000009901"),
                                Instant.parse("2026-07-11T12:00:00Z")));

        mockMvc.perform(
                        put("/api/system-settings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "monthlyContactLimit": 5,
                                          "sendRetryLimit": 4,
                                          "uninterestedExclusionDays": 120
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("System settings updated"))
                .andExpect(jsonPath("$.data.monthlyContactLimit").value(5))
                .andExpect(jsonPath("$.data.sendRetryLimit").value(4))
                .andExpect(jsonPath("$.data.uninterestedExclusionDays").value(120));

        ArgumentCaptor<UpdateSystemSettingsCommand> captor =
                ArgumentCaptor.forClass(UpdateSystemSettingsCommand.class);
        verify(systemSettingsService).updateSettings(captor.capture());
        assertThat(captor.getValue().monthlyContactLimit()).isEqualTo(5);
        assertThat(captor.getValue().sendRetryLimit()).isEqualTo(4);
        assertThat(captor.getValue().uninterestedExclusionDays()).isEqualTo(120);
    }

    private static SystemSettingsView sampleView() {
        return new SystemSettingsView(
                SystemSettings.SINGLETON_ID, 3, 3, 90, null, Instant.parse("2026-07-01T00:00:00Z"));
    }
}
