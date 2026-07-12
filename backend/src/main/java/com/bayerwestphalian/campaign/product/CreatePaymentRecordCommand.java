package com.bayerwestphalian.campaign.product;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePaymentRecordCommand(
        UUID customerId, UUID productOwnershipId, LocalDate dueDate, BigDecimal amountDue) {}
