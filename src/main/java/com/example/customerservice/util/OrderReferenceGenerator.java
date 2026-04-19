package com.example.customerservice.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;


@Component
public class OrderReferenceGenerator {
    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "CUS0000";
    private static final int RANDOM_LENGTH = 8;


    public OrderReferenceGenerator() {

    }

    public static String generate(){

        StringBuilder sb = new StringBuilder(PREFIX);
        for(int i=0;i<RANDOM_LENGTH;i++){
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();

    }
}
