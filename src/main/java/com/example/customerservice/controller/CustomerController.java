package com.example.customerservice.controller;

import com.example.customerservice.model.dto.CustomerRegistrationRequest;
import com.example.customerservice.service.CustomerRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {


    private final CustomerRegistrationService registrationService;


    public CustomerController(CustomerRegistrationService registrationService) {
        this.registrationService = registrationService;
    }




    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody CustomerRegistrationRequest request){

        String orderRefNo = registrationService.registerCustomer(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("orderRefNo",orderRefNo));
    }
}
