package com.bayerwestphalian.campaign.communication;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CommunicationModuleDocumentationTests {

    private static final Path DOC_PATH = Path.of("../docs/modules/communication-tracking.md");

    @Test
    void documentsCommunicationTrackingModuleAndProviders() throws Exception {
        String docContent = Files.readString(DOC_PATH, StandardCharsets.UTF_8);

        assertThat(docContent)
                .contains("# Communication Tracking Module")
                .contains("## Package Boundary")
                .contains("com.bayerwestphalian.campaign.communication")
                .contains("## Contact Event Model")
                .contains("ContactEventType")
                .contains("SENT")
                .contains("FAILED")
                .contains("UNSUBSCRIBED")
                .contains("IN_APP")
                .contains("## Send Result Event Recording")
                .contains("creates a `SENT` contact event")
                .contains("creates a `FAILED` contact event")
                .contains("## Provider Adapter Architecture")
                .contains("EmailProvider")
                .contains("SmsProvider")
                .contains("MockEmailProvider")
                .contains("MockSmsProvider")
                .contains("## Production Configuration Notes")
                .contains("app.providers.real-sending-enabled=false")
                .contains("REAL_SENDING_DISABLED");
    }
}
