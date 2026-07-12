/**
 * Authentication package for employee login, logout, token/session refresh, and current-user
 * identity.
 *
 * <p>Also hosts production edge security: CORS (item 540), HTTPS enforcement (item 541) via
 * {@link com.bayerwestphalian.campaign.auth.HttpsEnforcementFilter}, login rate limiting / lockout
 * (item 544) via {@link com.bayerwestphalian.campaign.auth.LoginAttemptTracker}, and API security
 * headers (item 545) via {@link com.bayerwestphalian.campaign.auth.ApiSecurityHeadersFilter}.
 */
package com.bayerwestphalian.campaign.auth;
