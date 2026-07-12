/**
 * Segment package for reusable audience criteria (FR-077), preview logic, eligibility-aware counts,
 * and exclusion summaries. Campaign Managers create and save reusable segments for later targeting.
 *
 * <p><strong>Production gate (item 208):</strong> segmentation must never return a final campaign
 * audience without eligibility checks. Use {@code SegmentService.previewSegment} (HTTP {@code POST
 * /api/segments/preview}) for contactable audiences; criteria-only matching is internal and is not
 * a public final-audience API.
 */
package com.bayerwestphalian.campaign.segment;
