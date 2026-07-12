package com.bayerwestphalian.campaign.communication;

import java.util.Map;

/** Email payload accepted by replaceable email provider adapters. */
public record EmailMessage(
        String to,
        String subject,
        String body,
        String correlationId,
        Map<String, String> metadata) {}
