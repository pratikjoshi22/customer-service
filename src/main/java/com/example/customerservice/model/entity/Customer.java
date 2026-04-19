package com.example.customerservice.model.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(unique = true)
    private String eventId;


    @Column(name = "transactionid", unique = true, nullable = false, length = 15)//already indexed
    private String orderRefNo;

    private String firstName;
    private String lastName;

    @Column(unique = true)
    private String email;

    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String gender;
    private String nationality;
    private String governmentId;
    private String governmentIdType;
    private String preferredLanguage;
    private boolean marketingOptIn;
    private String registeredVia;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<Address> addresses;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

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

    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }

    public boolean isMarketingOptIn() { return marketingOptIn; }
    public void setMarketingOptIn(boolean marketingOptIn) { this.marketingOptIn = marketingOptIn; }

    public String getRegisteredVia() { return registeredVia; }
    public void setRegisteredVia(String registeredVia) { this.registeredVia = registeredVia; }

    public List<Address> getAddresses() { return addresses; }
    public void setAddresses(List<Address> addresses) { this.addresses = addresses; }

    public String getOrderRefNo() {
        return orderRefNo;
    }

    public void setOrderRefNo(String orderRefNo) {
        this.orderRefNo = orderRefNo;
    }
}
