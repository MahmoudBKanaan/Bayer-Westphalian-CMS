package com.bayerwestphalian.campaign.communication;

import java.util.Map;

/** SMS payload accepted by replaceable SMS provider adapters. */
public record SmsMessage(
        String to, String body, String correlationId, Map<String, String> metadata) {}
