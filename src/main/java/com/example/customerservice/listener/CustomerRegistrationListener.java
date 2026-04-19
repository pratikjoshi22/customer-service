package com.example.customerservice.listener;

import com.example.customerservice.model.dto.CustomerRegistrationEvent;
import com.example.customerservice.service.CustomerRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class CustomerRegistrationListener {

    private static final Logger log = LoggerFactory.getLogger(CustomerRegistrationListener.class);

    private final CustomerRegistrationService registrationService;

    public CustomerRegistrationListener(CustomerRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

/*    @KafkaListener(
        topics = "${app.kafka.topic.customer-registration}",
        groupId = "${spring.kafka.consumer.group-id}"
    )*/
    public void onCustomerRegistration(
            @Payload CustomerRegistrationEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received registration event [{}] orderRef=[{}] on partition {} offset {}",
                event.getEventId(), event.getOrderRefNo(), partition, offset);

        registrationService.processRegistration(event);
    }
}
