package com.bayerwestphalian.campaign.campaign;

public enum EligibilityExclusionReason {
    DO_NOT_CONTACT("DO_NOT_CONTACT", "Customer has do-not-contact enabled"),
    MARKETING_OPT_OUT(
            "MARKETING_OPT_OUT",
            "Customer has withdrawn or rejected marketing consent"),
    INVALID_CONSENT("INVALID_CONSENT", "Customer does not have valid required consent"),
    DUPLICATE_CAMPAIGN_RECIPIENT(
            "DUPLICATE_CAMPAIGN_RECIPIENT",
            "Customer is already assigned to this campaign"),
    MONTHLY_CONTACT_LIMIT(
            "MONTHLY_CONTACT_LIMIT",
            "Customer has reached the monthly marketing contact limit");

    public static final String CODE_DO_NOT_CONTACT = "DO_NOT_CONTACT";
    public static final String CODE_MARKETING_OPT_OUT = "MARKETING_OPT_OUT";
    public static final String CODE_INVALID_CONSENT = "INVALID_CONSENT";
    public static final String CODE_DUPLICATE_CAMPAIGN_RECIPIENT =
            "DUPLICATE_CAMPAIGN_RECIPIENT";
    public static final String CODE_MONTHLY_CONTACT_LIMIT = "MONTHLY_CONTACT_LIMIT";

    private final String code;
    private final String explanation;

    EligibilityExclusionReason(String code, String explanation) {
        this.code = code;
        this.explanation = explanation;
    }

    public String code() {
        return code;
    }

    public String explanation() {
        return explanation;
    }
}
