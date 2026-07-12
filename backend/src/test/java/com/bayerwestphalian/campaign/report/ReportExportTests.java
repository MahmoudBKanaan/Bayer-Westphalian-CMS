package com.bayerwestphalian.campaign.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bayerwestphalian.campaign.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * KB item 435: ReportExport entity maps {@code report_exports} and tracks CSV/PDF export request
 * lifecycle (FR-109–FR-110).
 */
class ReportExportTests {

    private static final UUID REQUESTER_ID =
            UUID.fromString("10000000-0000-0000-0000-000000000435");

    @Test
    void mapsKbReportExportsTableAsJpaEntity() {
        assertThat(ReportExport.class.isAnnotationPresent(Entity.class)).isTrue();
        assertThat(ReportExport.class.getAnnotation(Table.class).name()).isEqualTo("report_exports");
    }

    @Test
    void providesProtectedNoArgsConstructorForJpa() throws Exception {
        Constructor<ReportExport> constructor = ReportExport.class.getDeclaredConstructor();
        assertThat(Modifier.isProtected(constructor.getModifiers())).isTrue();
    }

    @Test
    void mapsAllKbReportExportColumns() throws Exception {
        assertThat(field("id").isAnnotationPresent(Id.class)).isTrue();
        assertColumn("id", "id", false, false);
        assertColumn("reportName", "report_name", false, true);
        assertColumn("exportType", "export_type", false, true);
        assertColumn("status", "status", false, true);
        assertColumn("fileUrl", "file_url", true, true);
        assertColumn("requestedAt", "requested_at", false, false);
        assertColumn("completedAt", "completed_at", true, true);

        assertThat(field("reportName").getAnnotation(Column.class).length()).isEqualTo(255);
        assertThat(field("fileUrl").getAnnotation(Column.class).columnDefinition()).isEqualTo("text");
        assertThat(field("reportName").isAnnotationPresent(NotBlank.class)).isTrue();
        assertThat(field("reportName").isAnnotationPresent(Size.class)).isTrue();
        assertThat(field("exportType").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("status").isAnnotationPresent(NotNull.class)).isTrue();
        assertThat(field("requestedAt").isAnnotationPresent(NotNull.class)).isTrue();
    }

    @Test
    void mapsOptionalRequesterRelationship() throws Exception {
        Field requestedBy = field("requestedBy");
        ManyToOne manyToOne = requestedBy.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = requestedBy.getAnnotation(JoinColumn.class);

        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isTrue();
        assertThat(joinColumn.name()).isEqualTo("requested_by");
        assertThat(requestedBy.getType()).isEqualTo(User.class);
    }

    @Test
    void mapsKbPostgreSqlEnums() throws Exception {
        assertNativeEnumColumn("exportType", "report_export_type");
        assertNativeEnumColumn("status", "report_export_status");
    }

    @Test
    void declaresKbReportExportTypeValues() {
        assertThat(ReportExportType.values())
                .containsExactly(ReportExportType.CSV, ReportExportType.PDF);
    }

    @Test
    void declaresKbReportExportStatusValues() {
        assertThat(ReportExportStatus.values())
                .containsExactly(
                        ReportExportStatus.REQUESTED,
                        ReportExportStatus.COMPLETED,
                        ReportExportStatus.FAILED);
    }

    @Test
    void requestCreatesRequestedExportWithDefaults() {
        User requester = sampleRequester();

        ReportExport export =
                ReportExport.request(requester, "Campaign performance CSV", ReportExportType.CSV);

        assertThat(export.getRequestedBy()).isSameAs(requester);
        assertThat(export.getReportName()).isEqualTo("Campaign performance CSV");
        assertThat(export.getExportType()).isEqualTo(ReportExportType.CSV);
        assertThat(export.getStatus()).isEqualTo(ReportExportStatus.REQUESTED);
        assertThat(export.isRequested()).isTrue();
        assertThat(export.isCompleted()).isFalse();
        assertThat(export.isFailed()).isFalse();
        assertThat(export.getFileUrl()).isNull();
        assertThat(export.getCompletedAt()).isNull();
        assertThat(export.getId()).isNull();
        assertThat(export.getRequestedAt()).isNull();
    }

    @Test
    void requestWithoutRequesterAllowsNullRequestedBy() {
        ReportExport export = ReportExport.request("Audit history PDF", ReportExportType.PDF);

        assertThat(export.getRequestedBy()).isNull();
        assertThat(export.getRequestedByUserId()).isNull();
        assertThat(export.getReportName()).isEqualTo("Audit history PDF");
        assertThat(export.getExportType()).isEqualTo(ReportExportType.PDF);
        assertThat(export.getStatus()).isEqualTo(ReportExportStatus.REQUESTED);
    }

    @Test
    void requestTrimsReportName() {
        ReportExport export =
                ReportExport.request(sampleRequester(), "  Quarterly ROI  ", ReportExportType.PDF);

        assertThat(export.getReportName()).isEqualTo("Quarterly ROI");
    }

    @Test
    void requestRejectsMissingOrBlankReportName() {
        User requester = sampleRequester();

        assertThatThrownBy(() -> ReportExport.request(requester, null, ReportExportType.CSV))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Report name");

        assertThatThrownBy(() -> ReportExport.request(requester, "   ", ReportExportType.CSV))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Report name");
    }

    @Test
    void requestRejectsReportNameLongerThan255() {
        String tooLong = "R".repeat(256);

        assertThatThrownBy(() -> ReportExport.request(sampleRequester(), tooLong, ReportExportType.CSV))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("255");
    }

    @Test
    void requestRejectsMissingExportType() {
        assertThatThrownBy(() -> ReportExport.request(sampleRequester(), "Campaign CSV", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Export type");
    }

    @Test
    void markCompletedSetsFileUrlStatusAndCompletedAt() {
        ReportExport export =
                ReportExport.request(sampleRequester(), "Audit history PDF", ReportExportType.PDF);

        export.markCompleted("  s3://reports/audit-history.pdf  ");

        assertThat(export.getStatus()).isEqualTo(ReportExportStatus.COMPLETED);
        assertThat(export.isCompleted()).isTrue();
        assertThat(export.getFileUrl()).isEqualTo("s3://reports/audit-history.pdf");
        assertThat(export.getCompletedAt()).isNotNull();
    }

    @Test
    void markCompletedRejectsMissingOrBlankFileUrl() {
        ReportExport export =
                ReportExport.request(sampleRequester(), "Campaign CSV", ReportExportType.CSV);

        assertThatThrownBy(() -> export.markCompleted(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("File URL");
        assertThatThrownBy(() -> export.markCompleted("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File URL");
        assertThat(export.getStatus()).isEqualTo(ReportExportStatus.REQUESTED);
    }

    @Test
    void markFailedClearsDownloadFields() {
        ReportExport export =
                ReportExport.request(sampleRequester(), "Failed export CSV", ReportExportType.CSV);
        export.markCompleted("local://tmp/export.csv");

        export.markFailed();

        assertThat(export.getStatus()).isEqualTo(ReportExportStatus.FAILED);
        assertThat(export.isFailed()).isTrue();
        assertThat(export.getFileUrl()).isNull();
        assertThat(export.getCompletedAt()).isNull();
    }

    @Test
    void initializesIdAndRequestedAtBeforePersist() throws Exception {
        ReportExport export =
                ReportExport.request(sampleRequester(), "Campaign performance", ReportExportType.CSV);

        Method onCreate = ReportExport.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(export);

        assertThat(export.getId()).isNotNull();
        assertThat(export.getRequestedAt()).isNotNull();
        assertThat(export.getStatus()).isEqualTo(ReportExportStatus.REQUESTED);
    }

    @Test
    void getRequestedByUserIdReadsLinkedUserId() {
        User requester = sampleRequester();
        ReportExport export =
                ReportExport.request(requester, "Campaign CSV", ReportExportType.CSV);

        assertThat(export.getRequestedByUserId()).isEqualTo(REQUESTER_ID);
    }

    private static User sampleRequester() {
        User user = User.create("report.exporter@bayer-westphalian.test", "{noop}x", "Report Exporter");
        ReflectionTestUtils.setField(user, "id", REQUESTER_ID);
        return user;
    }

    private static Field field(String name) throws Exception {
        Field field = ReportExport.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void assertColumn(
            String fieldName, String columnName, boolean nullable, boolean updatable)
            throws Exception {
        Column column = field(fieldName).getAnnotation(Column.class);
        assertThat(column.name()).isEqualTo(columnName);
        assertThat(column.nullable()).isEqualTo(nullable);
        assertThat(column.updatable()).isEqualTo(updatable);
    }

    private static void assertNativeEnumColumn(String fieldName, String columnDefinition)
            throws Exception {
        Field enumField = field(fieldName);
        Enumerated enumerated = enumField.getAnnotation(Enumerated.class);
        JdbcTypeCode jdbcTypeCode = enumField.getAnnotation(JdbcTypeCode.class);
        Column column = enumField.getAnnotation(Column.class);

        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
        assertThat(jdbcTypeCode.value()).isEqualTo(SqlTypes.NAMED_ENUM);
        assertThat(column.columnDefinition()).isEqualTo(columnDefinition);
    }
}
