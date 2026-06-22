package com.app.service;

public interface SmsService {
    String[] sendVerificationCodeWithDypns(String phone);
    
    boolean sendVerificationCode(String phone, String code);
    
    String generateVerificationCode(int length);
    
    boolean verifyCodeWithDypns(String phone, String code);
}
