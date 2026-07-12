package com.bayerwestphalian.campaign.product;

import java.math.BigDecimal;
import java.time.Instant;

public record MarkPaymentPaidCommand(BigDecimal amountPaid, Instant paidAt) {}
