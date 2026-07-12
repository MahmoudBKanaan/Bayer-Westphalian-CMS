package com.bayerwestphalian.campaign.product;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdatePaymentRecordCommand(LocalDate dueDate, BigDecimal amountDue) {}
