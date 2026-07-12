package com.bayerwestphalian.campaign.communication;

/** Replaceable adapter boundary for outbound SMS delivery. */
public interface SmsProvider {

    SmsDeliveryResult send(SmsMessage message);
}
