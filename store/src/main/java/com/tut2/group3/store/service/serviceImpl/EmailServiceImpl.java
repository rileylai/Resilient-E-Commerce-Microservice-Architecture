package com.tut2.group3.store.service.serviceImpl;

import com.tut2.group3.store.dto.email.EmailRequestDto;
import com.tut2.group3.store.dto.email.EmailResponseDto;
import com.tut2.group3.store.service.EmailService;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {
    @Override
    public EmailResponseDto sendEmail(EmailRequestDto emailRequestDto) {
        return null;
    }
}
