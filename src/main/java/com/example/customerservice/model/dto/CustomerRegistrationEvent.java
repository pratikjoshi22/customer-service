package com.example.customerservice.model.dto;

import java.time.LocalDate;
import java.util.List;

public class CustomerRegistrationEvent {


    private String eventId;
    private String orderRefNo;
    private String source;

    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;

    private LocalDate dateOfBirth;
    private String gender;
    private String nationality;


    private String governmentId;
    private String governmentIdType;

    private List<AddressDto> addresses;

    private String preferredLanguage;
    private boolean marketingOptIn;

    private String registeredVia;
    private long registeredAtEpoch;

    public CustomerRegistrationEvent() {
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getOrderRefNo() {
        return orderRefNo;
    }

    public void setOrderRefNo(String orderRefNo) {
        this.orderRefNo = orderRefNo;
    }


    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getGovernmentId() { return governmentId; }
    public void setGovernmentId(String governmentId) { this.governmentId = governmentId; }

    public String getGovernmentIdType() { return governmentIdType; }
    public void setGovernmentIdType(String governmentIdType) { this.governmentIdType = governmentIdType; }

    public List<AddressDto> getAddresses() { return addresses; }
    public void setAddresses(List<AddressDto> addresses) { this.addresses = addresses; }

    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }

    public boolean isMarketingOptIn() { return marketingOptIn; }
    public void setMarketingOptIn(boolean marketingOptIn) { this.marketingOptIn = marketingOptIn; }

    public String getRegisteredVia() { return registeredVia; }
    public void setRegisteredVia(String registeredVia) { this.registeredVia = registeredVia; }

    public long getRegisteredAtEpoch() { return registeredAtEpoch; }
    public void setRegisteredAtEpoch(long registeredAtEpoch) { this.registeredAtEpoch = registeredAtEpoch; }
}
