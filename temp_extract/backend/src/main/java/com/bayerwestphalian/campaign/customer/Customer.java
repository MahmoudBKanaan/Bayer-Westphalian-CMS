package com.bayerwestphalian.campaign.customer;

import com.bayerwestphalian.campaign.common.domain.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "customers")
public class Customer extends SoftDeletableEntity {

    @NotNull @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "customer_type", nullable = false, columnDefinition = "customer_type")
    private CustomerType customerType;

    @NotBlank @Size(max = 100) @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank @Size(max = 100) @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Email @Size(max = 255) @Column(name = "email")
    private String email;

    @Size(max = 50) @Pattern(regexp = "^\\+?[0-9 ()-]{7,50}$")
    @Column(name = "phone", length = 50)
    private String phone;

    @Size(max = 255) @Column(name = "address_line")
    private String addressLine;

    @Size(max = 100) @Column(name = "city", length = 100)
    private String city;

    @Size(max = 100) @Column(name = "country", length = 100)
    private String country;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Convert(converter = CustomerAgeGroupConverter.class)
    @ColumnTransformer(write = "?::customer_age_group")
    @Column(name = "age_group", columnDefinition = "customer_age_group")
    private CustomerAgeGroup ageGroup;

    @NotNull @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "customer_status")
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Column(name = "do_not_contact", nullable = false)
    private boolean doNotContact;

    @Size(max = 100) @Column(name = "source", length = 100)
    private String source;

    protected Customer() {}

    private Customer(CustomerType customerType, String firstName, String lastName) {
        this.customerType = customerType;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public static Customer create(CustomerType customerType, String firstName, String lastName) {
        return new Customer(customerType, firstName, lastName);
    }

    public CustomerType getCustomerType() {
        return customerType;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public CustomerAgeGroup getAgeGroup() {
        return ageGroup;
    }

    public CustomerStatus getStatus() {
        return status;
    }

    public boolean isDoNotContact() {
        return doNotContact;
    }

    public String getSource() {
        return source;
    }

    public boolean isActive() {
        return status == CustomerStatus.ACTIVE && !isDeleted();
    }

    public boolean canBeContacted() {
        return !doNotContact && !isDeleted();
    }

    public void rename(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public void updateContactDetails(String email, String phone) {
        this.email = email;
        this.phone = phone;
    }

    public void updateAddress(String addressLine, String city, String country) {
        this.addressLine = addressLine;
        this.city = city;
        this.country = country;
    }

    public void updateDemographics(LocalDate dateOfBirth, CustomerAgeGroup ageGroup) {
        this.dateOfBirth = dateOfBirth;
        this.ageGroup = ageGroup;
    }

    public void changeStatus(CustomerStatus status) {
        this.status = status;
    }

    public void markDoNotContact() {
        doNotContact = true;
    }

    public void allowContact() {
        doNotContact = false;
    }

    public void recordSource(String source) {
        this.source = source;
    }
}
