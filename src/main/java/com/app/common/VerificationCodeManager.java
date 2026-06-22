package com.app.common;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class VerificationCodeManager {
    private final Map<String, VerificationCodeInfo> codeMap = new HashMap<>();
    private static final long EXPIRE_TIME = 300;

    public void storeCode(String phone, String code) {
        long expireAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(EXPIRE_TIME);
        codeMap.put(phone, new VerificationCodeInfo(code, expireAt));
    }

    public boolean verifyCode(String phone, String code) {
        VerificationCodeInfo info = codeMap.get(phone);
        if (info == null) {
            return false;
        }
        if (System.currentTimeMillis() > info.expireAt) {
            codeMap.remove(phone);
            return false;
        }
        boolean isValid = info.code.equals(code);
        if (isValid) {
            codeMap.remove(phone);
        }
        return isValid;
    }

    public void storeCodeWithAccessCode(String phone, String code, String accessCode) {
        long expireAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(EXPIRE_TIME);
        codeMap.put(phone, new VerificationCodeInfo(code, expireAt, accessCode));
    }

    public String getAccessCode(String phone) {
        VerificationCodeInfo info = codeMap.get(phone);
        if (info == null) {
            return null;
        }
        if (System.currentTimeMillis() > info.expireAt) {
            codeMap.remove(phone);
            return null;
        }
        return info.accessCode;
    }

    private static class VerificationCodeInfo {
        private final String code;
        private final long expireAt;
        private final String accessCode;

        private VerificationCodeInfo(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
            this.accessCode = null;
        }

        private VerificationCodeInfo(String code, long expireAt, String accessCode) {
            this.code = code;
            this.expireAt = expireAt;
            this.accessCode = accessCode;
        }
    }
}
