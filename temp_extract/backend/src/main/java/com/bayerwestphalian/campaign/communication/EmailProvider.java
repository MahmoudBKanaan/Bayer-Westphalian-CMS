package com.bayerwestphalian.campaign.communication;

/** Replaceable adapter boundary for outbound email delivery. */
public interface EmailProvider {

    EmailDeliveryResult send(EmailMessage message);
}
