package com.tut2.group3.store.service;

import com.tut2.group3.store.dto.email.EmailRequestDto;
import com.tut2.group3.store.dto.email.EmailResponseDto;

public interface EmailService {

    EmailResponseDto sendEmail(EmailRequestDto emailRequestDto);

}
