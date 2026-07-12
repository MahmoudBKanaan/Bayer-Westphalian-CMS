package com.bayerwestphalian.campaign.customer;

import java.util.Arrays;

public enum CustomerAgeGroup {
    MINOR("MINOR"),
    AGE_18_25("18_25"),
    AGE_26_40("26_40"),
    AGE_41_60("41_60"),
    AGE_60_PLUS("60_PLUS");

    private final String databaseValue;

    CustomerAgeGroup(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public static CustomerAgeGroup fromDatabaseValue(String databaseValue) {
        return Arrays.stream(values())
                .filter(ageGroup -> ageGroup.databaseValue.equals(databaseValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown customer age group"));
    }
}
