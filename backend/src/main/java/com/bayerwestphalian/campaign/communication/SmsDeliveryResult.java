package com.bayerwestphalian.campaign.communication;

/** Provider response for an attempted SMS delivery. */
public record SmsDeliveryResult(
        boolean accepted, String providerMessageId, String errorCode, String errorMessage) {

    public static SmsDeliveryResult accepted(String providerMessageId) {
        return new SmsDeliveryResult(true, providerMessageId, null, null);
    }

    public static SmsDeliveryResult failed(String errorCode, String errorMessage) {
        return new SmsDeliveryResult(false, null, errorCode, errorMessage);
    }
}
