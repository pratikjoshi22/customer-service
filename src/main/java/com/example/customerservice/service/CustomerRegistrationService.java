package com.example.customerservice.service;

import com.example.customerservice.model.dto.CustomerRegistrationEvent;
import com.example.customerservice.model.dto.CustomerRegistrationRequest;
import com.example.customerservice.model.entity.Address;
import com.example.customerservice.model.entity.Customer;
import com.example.customerservice.repository.CustomerRepository;
import com.example.customerservice.util.OrderReferenceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(CustomerRegistrationService.class);

    private final CustomerRepository customerRepository;
    private final KafkaTemplate<String, CustomerRegistrationEvent> kafkaTemplate;


    @Value("${app.kafka.topic.customer-registration}")
    private String customerRegistrationTopic;


    public CustomerRegistrationService(CustomerRepository customerRepository, KafkaTemplate<String, CustomerRegistrationEvent> kafkaTemplate) {
        this.customerRepository = customerRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public void processRegistration(CustomerRegistrationEvent event) {
        if (customerRepository.existsByEmail(event.getEmail())) {
            log.warn("Customer with email {} already exists, skipping event {}",
                    event.getEmail(), event.getEventId());
            return;
        }

        Customer customer = mapToEntity(event);
        customerRepository.save(customer);

        log.info("Successfully registered customer {} {} [orderReference={}]",
                customer.getFirstName(), customer.getLastName(), customer.getOrderRefNo());
    }


    //not in use
    private Customer mapToEntity(CustomerRegistrationEvent event) {
        Customer c = new Customer();
        c.setEventId(event.getEventId());
        c.setOrderRefNo(OrderReferenceGenerator.generate());
        c.setFirstName(event.getFirstName());
        c.setLastName(event.getLastName());
        c.setEmail(event.getEmail());
        c.setPhoneNumber(event.getPhoneNumber());
        c.setDateOfBirth(event.getDateOfBirth());
        c.setGender(event.getGender());
        c.setNationality(event.getNationality());
        c.setGovernmentId(event.getGovernmentId());
        c.setGovernmentIdType(event.getGovernmentIdType());
        c.setPreferredLanguage(event.getPreferredLanguage());
        c.setMarketingOptIn(event.isMarketingOptIn());
        c.setRegisteredVia(event.getRegisteredVia());

        if (event.getAddresses() != null) {
            c.setAddresses(event.getAddresses().stream().map(dto -> {
                Address addr = new Address();
                addr.setType(dto.getType());
                addr.setLine1(dto.getLine1());
                addr.setLine2(dto.getLine2());
                addr.setCity(dto.getCity());
                addr.setState(dto.getState());
                addr.setPostalCode(dto.getPostalCode());
                addr.setCountry(dto.getCountry());
                addr.setCustomer(c);
                return addr;
            }).collect(Collectors.toList()));
        }

        return c;
    }

    @Transactional
    public String registerCustomer(CustomerRegistrationRequest request) {
        //check duplicate entry with email to ensure unique data

        if (customerRepository.existsByEmail(request.getEmail())){
            throw new IllegalStateException("Customer with email " + request.getEmail() + " already exists");
        }

        //generated ORN and EventID
        String orderRefNo = OrderReferenceGenerator.generate();

        String eventId = UUID.randomUUID().toString();

        Customer customer = mapRequestToEntity(request,orderRefNo,eventId);

        customerRepository.save(customer);




        //sending event on kafka topic with topic name,key(order ref no), and event details
        CustomerRegistrationEvent  event = mapRequestToEvent(request, orderRefNo, eventId);

        kafkaTemplate.send(customerRegistrationTopic,orderRefNo,event);

        log.info("Registered customer {} {} [orderRefNo={}], published to Kafka",
                customer.getFirstName(), customer.getLastName(), orderRefNo);

        return orderRefNo;


    }

    private CustomerRegistrationEvent mapRequestToEvent(CustomerRegistrationRequest request, String orderRefNo, String eventId) {

        CustomerRegistrationEvent event = new CustomerRegistrationEvent();

        event.setOrderRefNo(orderRefNo);
        event.setEventId(eventId);
        event.setSource(request.getSource());
        event.setFirstName(request.getFirstName());
        event.setLastName(request.getLastName());
        event.setEmail(request.getEmail());
        event.setPhoneNumber(request.getPhoneNumber());
        event.setDateOfBirth(request.getDateOfBirth());
        event.setGender(request.getGender());
        event.setNationality(request.getNationality());
        event.setGovernmentId(request.getGovernmentId());
        event.setGovernmentIdType(request.getGovernmentIdType());
        event.setAddresses(request.getAddresses());
        event.setPreferredLanguage(request.getPreferredLanguage());
        event.setMarketingOptIn(request.isMarketingOptIn());
        event.setRegisteredVia(request.getRegisteredVia());
        event.setRegisteredAtEpoch(System.currentTimeMillis());
        return event;




    }

    private Customer mapRequestToEntity(CustomerRegistrationRequest request, String orderRefNo, String eventId) {

        Customer c = new Customer();

        c.setOrderRefNo(orderRefNo);
        c.setEventId(eventId);

        c.setFirstName(request.getFirstName());
        c.setLastName(request.getLastName());
        c.setEmail(request.getEmail());
        c.setPhoneNumber(request.getPhoneNumber());
        c.setDateOfBirth(request.getDateOfBirth());
        c.setGender(request.getGender());
        c.setNationality(request.getNationality());
        c.setGovernmentId(request.getGovernmentId());
        c.setGovernmentIdType(request.getGovernmentIdType());
        c.setPreferredLanguage(request.getPreferredLanguage());
        c.setMarketingOptIn(request.isMarketingOptIn());
        c.setRegisteredVia(request.getRegisteredVia());
        if (request.getAddresses() != null) {
            c.setAddresses(request.getAddresses().stream().map(dto -> {
                Address addr = new Address();
                addr.setType(dto.getType());
                addr.setLine1(dto.getLine1());
                addr.setLine2(dto.getLine2());
                addr.setCity(dto.getCity());
                addr.setState(dto.getState());
                addr.setPostalCode(dto.getPostalCode());
                addr.setCountry(dto.getCountry());
                addr.setCustomer(c);
                return addr;
            }).collect(Collectors.toList()));
        }



        return c;



    }
}
