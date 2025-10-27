package com.tut2.group3.store;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import java.util.TimeZone;

@SpringBootApplication
@EnableFeignClients
@MapperScan("com.tut2.group3.store.mapper")
public class StoreApplication {

    public static void main(String[] args) {
        // Set default timezone to Australia/Sydney for consistent time handling
        TimeZone.setDefault(TimeZone.getTimeZone("Australia/Sydney"));
        SpringApplication.run(StoreApplication.class, args);
    }

}
