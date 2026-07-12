package com.bayerwestphalian.campaign.communication;

/** Provider response for an attempted email delivery. */
public record EmailDeliveryResult(
        boolean accepted, String providerMessageId, String errorCode, String errorMessage) {

    public static EmailDeliveryResult accepted(String providerMessageId) {
        return new EmailDeliveryResult(true, providerMessageId, null, null);
    }

    public static EmailDeliveryResult failed(String errorCode, String errorMessage) {
        return new EmailDeliveryResult(false, null, errorCode, errorMessage);
    }
}
