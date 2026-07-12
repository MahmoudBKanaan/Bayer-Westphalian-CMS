/**
 * Shared API response contracts for KB-aligned REST controllers.
 *
 * <p>Item 538: secure error responses via {@link
 * com.bayerwestphalian.campaign.common.api.ErrorResponse}, {@link
 * com.bayerwestphalian.campaign.common.api.GlobalExceptionHandler}, and {@link
 * com.bayerwestphalian.campaign.common.api.SecureErrorResponses}.
 *
 * <p>Item 539: production profile hides stack traces via {@link
 * com.bayerwestphalian.campaign.common.api.ProductionErrorSafetyConfiguration} and {@code
 * application-prod.yml} {@code server.error.*} settings.
 *
 * <p>Item 546: server-side API error logging without secrets via {@link
 * com.bayerwestphalian.campaign.common.api.SafeApiErrorLogger}.
 */
package com.bayerwestphalian.campaign.common.api;
