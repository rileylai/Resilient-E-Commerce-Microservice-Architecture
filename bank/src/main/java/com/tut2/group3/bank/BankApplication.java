package com.tut2.group3.bank;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@MapperScan("com.tut2.group3.bank.repository")
public class BankApplication {

    // // test
    // @Value("${spring.security.user.name}")
    // private String username;

    // @Value("${spring.security.user.password}")
    // private String password;



    public static void main(String[] args) {
        SpringApplication.run(BankApplication.class, args);
    }



    // @PostConstruct
    // public void printSecurityProps() {
    //     System.out.println("✅ Loaded security user.name: " + username);
    //     System.out.println("✅ Loaded security password: " + password);
    // }

}