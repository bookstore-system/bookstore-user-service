package com.notfound.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class BookstoreUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookstoreUserServiceApplication.class, args);

    }

}
