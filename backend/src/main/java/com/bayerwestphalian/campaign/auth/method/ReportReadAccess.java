package com.bayerwestphalian.campaign.auth.method;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Method security for campaign report export and history (KB FR-109–FR-110 / item 458 / Sprint 16
 * critical item 663).
 *
 * <p>Delegates to {@code @authz.canViewReports()} (Admin, BI Analyst, Campaign Manager, Marketing
 * Analyst, Executive Viewer). Unauthorized users must not export restricted reports.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@authz.canViewReports()")
public @interface ReportReadAccess {}
