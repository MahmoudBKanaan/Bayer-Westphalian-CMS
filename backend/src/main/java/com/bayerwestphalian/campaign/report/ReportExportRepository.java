package com.bayerwestphalian.campaign.report;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence for report export history (KB {@code report_exports} / item 439).
 *
 * <p>Stores and retrieves export request rows created by {@link ReportService} CSV/PDF/audit
 * exports. Supports listing by requester, status, and export type for history screens.
 */
public interface ReportExportRepository extends JpaRepository<ReportExport, UUID> {

    /** Newest-first full export history (item 439). */
    List<ReportExport> findAllByOrderByRequestedAtDesc();

    List<ReportExport> findByRequestedBy_IdOrderByRequestedAtDesc(UUID requestedByUserId);

    List<ReportExport> findByStatusOrderByRequestedAtDesc(ReportExportStatus status);

    List<ReportExport> findByExportTypeOrderByRequestedAtDesc(ReportExportType exportType);

    /** KB-friendly history listing for a requesting user (item 439). */
    default List<ReportExport> findByRequestedByUserId(UUID requestedByUserId) {
        return findByRequestedBy_IdOrderByRequestedAtDesc(requestedByUserId);
    }
}
