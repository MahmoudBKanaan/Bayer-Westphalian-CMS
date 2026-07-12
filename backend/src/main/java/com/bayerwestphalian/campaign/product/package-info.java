/**
 * Product package for insurance and investment product management, ownership, payments, and
 * product-change requests (KB epic E10–E11).
 *
 * <p>Sensitive product mutations are audited through {@link
 * com.bayerwestphalian.campaign.audit.AuditService}:
 *
 * <ul>
 *   <li>Item 527 — product catalog changes ({@code CREATE}/{@code UPDATE}/{@code DELETE} on entity
 *       type {@code products} via {@link com.bayerwestphalian.campaign.product.ProductService})
 *   <li>Ownership, payment, and product-change request audits — see {@code
 *       docs/modules/product-audit-logging.md}
 * </ul>
 */
package com.bayerwestphalian.campaign.product;
