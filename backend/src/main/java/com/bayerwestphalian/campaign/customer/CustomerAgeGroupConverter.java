package com.bayerwestphalian.campaign.customer;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CustomerAgeGroupConverter implements AttributeConverter<CustomerAgeGroup, String> {

    @Override
    public String convertToDatabaseColumn(CustomerAgeGroup ageGroup) {
        return ageGroup == null ? null : ageGroup.getDatabaseValue();
    }

    @Override
    public CustomerAgeGroup convertToEntityAttribute(String databaseValue) {
        return databaseValue == null ? null : CustomerAgeGroup.fromDatabaseValue(databaseValue);
    }
}
