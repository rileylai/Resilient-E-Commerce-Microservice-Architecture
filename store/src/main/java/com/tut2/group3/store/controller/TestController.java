package com.tut2.group3.store.controller;

import com.tut2.group3.store.dto.UserDto;
import com.tut2.group3.store.exception.BusinessException;
import com.tut2.group3.store.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private UserService userService;

    @GetMapping("/secure")
    public String secureTest() {
        return "secure check";
    }

    @GetMapping("/search")
    public UserDto findUserByName(@RequestParam String username) {

        UserDto dto = userService.findByUsername(username);

        if(dto == null){
            throw new BusinessException(405, "User not found");
        }
        return dto;
    }
}
