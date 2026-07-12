package com.bayerwestphalian.campaign.ai;

import jakarta.validation.constraints.Size;

/**
 * Optional body when a human approves a stored AI recommendation (KB COMP-005 / AI-005 / item 471 /
 * item 482).
 *
 * <p>Approver identity comes from the authenticated principal, not this body. Notes are optional
 * audit context only.
 */
public record ApproveAiRecommendationRequest(@Size(max = 1000) String reviewNotes) {}
