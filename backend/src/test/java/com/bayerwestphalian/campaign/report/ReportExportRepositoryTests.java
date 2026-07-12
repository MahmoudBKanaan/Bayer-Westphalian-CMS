package com.bayerwestphalian.campaign.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * KB item 439: ReportExportRepository declares history query methods for {@code report_exports}.
 */
class ReportExportRepositoryTests {

    @Test
    void extendsJpaRepositoryForReportExportAggregate() {
        assertThat(JpaRepository.class).isAssignableFrom(ReportExportRepository.class);

        ParameterizedType repositoryType =
                (ParameterizedType)
                        List.of(ReportExportRepository.class.getGenericInterfaces()).stream()
                                .filter(ParameterizedType.class::isInstance)
                                .findFirst()
                                .orElseThrow();

        assertThat(repositoryType.getRawType()).isEqualTo(JpaRepository.class);
        assertThat(repositoryType.getActualTypeArguments())
                .containsExactly(ReportExport.class, UUID.class);
    }

    @Test
    void declaresHistoryListingMethods() throws Exception {
        assertListOfReportExport(
                ReportExportRepository.class.getMethod("findAllByOrderByRequestedAtDesc"));
        assertListOfReportExport(
                ReportExportRepository.class.getMethod(
                        "findByRequestedBy_IdOrderByRequestedAtDesc", UUID.class));
        assertListOfReportExport(
                ReportExportRepository.class.getMethod(
                        "findByStatusOrderByRequestedAtDesc", ReportExportStatus.class));
        assertListOfReportExport(
                ReportExportRepository.class.getMethod(
                        "findByExportTypeOrderByRequestedAtDesc", ReportExportType.class));
    }

    @Test
    void declaresKbFriendlyRequesterLookupDefault() throws Exception {
        Method method =
                ReportExportRepository.class.getMethod("findByRequestedByUserId", UUID.class);
        assertThat(method.isDefault()).isTrue();
        assertListOfReportExport(method);
    }

    private static void assertListOfReportExport(Method method) {
        assertThat(method.getReturnType()).isEqualTo(List.class);
        ParameterizedType generic = (ParameterizedType) method.getGenericReturnType();
        assertThat(generic.getActualTypeArguments()[0]).isEqualTo(ReportExport.class);
    }
}
