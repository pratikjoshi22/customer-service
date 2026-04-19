package com.example.customerservice.model.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;


@Getter
@Setter
public class CustomerRegistrationRequest {
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
}
