package com.bayerwestphalian.campaign.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.bayerwestphalian.campaign.user.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** KB item 439: ReportExportView maps stored export history fields for API responses. */
class ReportExportViewTests {

    private static final UUID EXPORT_ID = UUID.fromString("56000000-0000-0000-0000-000000000436");
    private static final UUID REQUESTER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000436");

    @Test
    void fromMapsEntityFields() {
        User requester =
                User.create("report.user@bayer-westphalian.test", "{noop}x", "Report User");
        ReflectionTestUtils.setField(requester, "id", REQUESTER_ID);

        ReportExport export =
                ReportExport.request(requester, "Campaign CSV: Demo", ReportExportType.CSV);
        ReflectionTestUtils.setField(export, "id", EXPORT_ID);
        ReflectionTestUtils.setField(
                export, "requestedAt", Instant.parse("2026-07-11T12:00:00Z"));
        export.markCompleted("local://reports/demo.csv");

        ReportExportView view = ReportExportView.from(export);

        assertThat(view.id()).isEqualTo(EXPORT_ID);
        assertThat(view.requestedByUserId()).isEqualTo(REQUESTER_ID);
        assertThat(view.reportName()).isEqualTo("Campaign CSV: Demo");
        assertThat(view.exportType()).isEqualTo(ReportExportType.CSV);
        assertThat(view.status()).isEqualTo(ReportExportStatus.COMPLETED);
        assertThat(view.fileUrl()).isEqualTo("local://reports/demo.csv");
        assertThat(view.requestedAt()).isEqualTo(Instant.parse("2026-07-11T12:00:00Z"));
        assertThat(view.completedAt()).isNotNull();
    }
}
