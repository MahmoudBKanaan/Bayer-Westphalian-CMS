package com.bayerwestphalian.campaign.product;

import java.time.LocalDate;

public record UpdateProductOwnershipCommand(LocalDate expirationDate, String policyNumber) {}