package com.bayerwestphalian.campaign.auth.method;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Method security for creating/saving reusable segments (KB FR-077, Campaign Manager segment
 * creation permissions). Allowed roles are Admin and Campaign Manager via {@code
 * @authz.canCreateSegments()}. Prefer this meta-annotation on create endpoints/services so create
 * stays distinct from edit/delete ({@code canManageSegments}).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@authz.canCreateSegments()")
public @interface SegmentCreateAccess {}
