package com.tut2.group3.store.dto.email;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailRequestDto {
    public Long userId;
    public Long orderId;
    public String emailAddress;
    public String message;
}
