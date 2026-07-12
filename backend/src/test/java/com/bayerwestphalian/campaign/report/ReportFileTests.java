package com.bayerwestphalian.campaign.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** KB item 436: ReportFile defensive copy of content bytes. */
class ReportFileTests {

    @Test
    void contentIsDefensivelyCopied() {
        byte[] original = "a,b\n1,2\n".getBytes();
        ReportExportView export =
                new ReportExportView(
                        UUID.fromString("56000000-0000-0000-0000-000000000436"),
                        null,
                        "Demo",
                        ReportExportType.CSV,
                        ReportExportStatus.COMPLETED,
                        "local://demo.csv",
                        Instant.parse("2026-07-11T12:00:00Z"),
                        Instant.parse("2026-07-11T12:01:00Z"));

        ReportFile file =
                new ReportFile("demo.csv", ReportFile.CSV_CONTENT_TYPE, original, export);

        original[0] = 'Z';
        assertThat(file.content()[0]).isEqualTo((byte) 'a');

        byte[] returned = file.content();
        returned[0] = 'Y';
        assertThat(file.content()[0]).isEqualTo((byte) 'a');
        assertThat(file.contentLength()).isEqualTo(8);
        assertThat(file.filename()).isEqualTo("demo.csv");
        assertThat(file.export()).isSameAs(export);
    }
}
