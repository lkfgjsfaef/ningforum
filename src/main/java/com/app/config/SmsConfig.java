package com.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SmsConfig {
    @Value("${sms.accessKeyId:disabled}")
    private String accessKeyId;
    
    @Value("${sms.accessKeySecret:disabled}")
    private String accessKeySecret;
    
    @Value("${sms.signName:disabled}")
    private String signName;
    
    @Value("${sms.templateCode:disabled}")
    private String templateCode;
    
    @Value("${sms.regionId:disabled}")
    private String regionId;
    
    @Value("${sms.endpoint:disabled}")
    private String endpoint;
    
    @Value("${sms.templateParamName:code}")
    private String templateParamName;

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getSignName() {
        return signName;
    }

    public void setSignName(String signName) {
        this.signName = signName;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public String getTemplateParamName() {
        return templateParamName;
    }
    
    public boolean isEnabled() {
        return !"disabled".equals(accessKeyId) && 
               !"disabled".equals(accessKeySecret) && 
               !"disabled".equals(signName) && 
               !"disabled".equals(templateCode) && 
               !"disabled".equals(regionId) && 
               !"disabled".equals(endpoint);
    }
    
    public void setTemplateParamName(String templateParamName) {
        this.templateParamName = templateParamName;
    }
}
