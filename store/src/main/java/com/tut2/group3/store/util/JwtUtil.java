package com.tut2.group3.store.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

   private final String secret;
   private final int expireHours;

   public JwtUtil(@Value("${jwt.secret}") String secret,
                  @Value("${jwt.expire-hours}") int expireHours) {
       this.secret = secret;
       this.expireHours = expireHours;
   }

   public String generateToken(Map<String, Object> claims){



       Date expireDate = new Date(System.currentTimeMillis()+(long) expireHours * 60 * 60 * 1000);
       return JWT.create()
               .withPayload(claims)
               .withExpiresAt(expireDate)
               .sign(Algorithm.HMAC256(secret));
   }

   public DecodedJWT parseToken(String token) throws JWTVerificationException {
       return JWT.require(Algorithm.HMAC256(secret))
               .build()
               .verify(token);
   }
   //return null instead of exception
   public DecodedJWT safeParseToken(String token) {
       try {
           return parseToken(token);
       } catch (Exception e) {
           return null;
       }
   }
}
