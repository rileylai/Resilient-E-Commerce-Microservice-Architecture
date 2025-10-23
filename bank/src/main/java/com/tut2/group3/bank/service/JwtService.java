package com.tut2.group3.bank.service;

public interface JwtService {

    String generateToken(Long userId, String role);

}
