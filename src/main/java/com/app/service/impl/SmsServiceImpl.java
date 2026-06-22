package com.app.service.impl;

import com.app.common.VerificationCodeManager;
import com.app.config.SmsConfig;
import com.app.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.TimeZone;
import java.util.logging.Logger;

@Service
public class SmsServiceImpl implements SmsService {

    private static final Logger logger = Logger.getLogger(SmsServiceImpl.class.getName());

    @Autowired
    private SmsConfig smsConfig;
    
    @Autowired
    private VerificationCodeManager verificationCodeManager;

    @Override
    public String[] sendVerificationCodeWithDypns(String phone) {
        // 检查是否禁用短信服务
        if ("disabled".equalsIgnoreCase(smsConfig.getAccessKeyId())) {
            logger.info("短信服务已禁用，直接返回成功");
            return new String[]{"000000", "disabled"};
        }
        
        try {
            if (smsConfig.getAccessKeyId() == null || smsConfig.getAccessKeyId().isEmpty() ||
                smsConfig.getAccessKeySecret() == null || smsConfig.getAccessKeySecret().isEmpty()) {
                logger.severe("短信服务配置错误：AccessKeyId 或 AccessKeySecret 为空");
                return null;
            }
            
            if (smsConfig.getSignName() == null || smsConfig.getSignName().trim().isEmpty()) {
                logger.severe("短信服务配置错误：签名名称为空");
                return null;
            }
            
            if (smsConfig.getTemplateCode() == null || smsConfig.getTemplateCode().trim().isEmpty()) {
                logger.severe("短信服务配置错误：模板代码为空");
                return null;
            }

            logger.info("========== 短信服务配置信息 ==========");
            logger.info("AccessKeyId: " + (smsConfig.getAccessKeyId().length() > 10 ? 
                smsConfig.getAccessKeyId().substring(0, 10) + "..." : smsConfig.getAccessKeyId()));
            logger.info("签名名称: " + smsConfig.getSignName() + " (长度: " + smsConfig.getSignName().length() + ")");
            logger.info("模板代码: " + smsConfig.getTemplateCode());
            logger.info("======================================");
            
            logger.info("开始发送短信验证码，手机号: " + phone);
            logger.info("使用 dypns（号码认证服务）API");

            String endpoint = "https://dypnsapi.aliyuncs.com";
            String action = "SendSmsVerifyCode";
            String version = "2017-05-25";
            
            Map<String, String> params = new HashMap<>();
            params.put("Action", action);
            params.put("Version", version);
            params.put("AccessKeyId", smsConfig.getAccessKeyId());
            params.put("Format", "JSON");
            params.put("SignatureMethod", "HMAC-SHA1");
            params.put("SignatureVersion", "1.0");
            params.put("SignatureNonce", UUID.randomUUID().toString());
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String timestamp = sdf.format(new Date());
            params.put("Timestamp", timestamp);
            logger.info("生成的时间戳（UTC）: " + timestamp);
            
            params.put("PhoneNumber", phone);
            
            String signName = smsConfig.getSignName() != null ? smsConfig.getSignName().trim() : "";
            if (signName.isEmpty()) {
                logger.severe("签名名称为空，无法发送短信");
                return null;
            }
            
            logger.info("========== 签名名称调试信息 ==========");
            logger.info("签名名称（原始）: [" + signName + "]");
            logger.info("签名名称字节（UTF-8）: " + java.util.Arrays.toString(signName.getBytes(StandardCharsets.UTF_8)));
            logger.info("签名名称字节（ISO-8859-1）: " + java.util.Arrays.toString(signName.getBytes(StandardCharsets.ISO_8859_1)));
            
            boolean needsFix = false;
            try {
                String testFix = new String(signName.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                if (testFix.matches(".*[\\u4e00-\\u9fa5].*") && testFix.length() <= 20) {
                    signName = testFix;
                    needsFix = true;
                    logger.info("检测到编码问题，已修复签名名称: [" + signName + "]");
                }
            } catch (Exception e) {
                logger.warning("签名名称编码修复尝试失败: " + e.getMessage());
            }
            
            logger.info("签名名称（最终）: [" + signName + "]");
            logger.info("======================================");
            
            params.put("SignName", signName);
            
            String templateCode = smsConfig.getTemplateCode() != null ? smsConfig.getTemplateCode().trim() : "";
            if (templateCode.isEmpty()) {
                logger.severe("模板代码为空，无法发送短信");
                return null;
            }
            params.put("TemplateCode", templateCode);
            
            int expireMinutes = 5;
            
            String paramName = smsConfig.getTemplateParamName() != null && !smsConfig.getTemplateParamName().isEmpty() 
                ? smsConfig.getTemplateParamName() : "code";
            
            String templateParam = String.format("{\"%s\":\"##code##\",\"min\":\"%d\"}", paramName, expireMinutes);
            params.put("TemplateParam", templateParam);
            logger.info("模板参数名称: " + paramName);
            logger.info("模板参数 JSON: " + templateParam);
            
            params.put("CountryCode", "86");
            
            logger.info("请求参数 - 手机号: " + phone + ", 签名: " + smsConfig.getSignName() + 
                       ", 模板: " + smsConfig.getTemplateCode());
            logger.info("模板参数: " + templateParam);
            
            String signature = generateSignature(params, smsConfig.getAccessKeySecret(), "POST");
            params.put("Signature", signature);
            
            String responseStr = sendHttpRequest(endpoint, params);
            logger.info("========== 发送验证码 API 完整响应 ==========");
            logger.info("原始响应字符串: " + responseStr);
            logger.info("==========================================");
            
            Map<String, Object> response = parseJsonResponse(responseStr);
            
            if (response != null) {
                logger.info("========== 解析后的响应结构 ==========");
                logger.info("响应中的所有键: " + response.keySet());
                for (Map.Entry<String, Object> entry : response.entrySet()) {
                    logger.info("键: " + entry.getKey() + ", 值类型: " + 
                        (entry.getValue() != null ? entry.getValue().getClass().getName() : "null") + 
                        ", 值: " + entry.getValue());
                }
                logger.info("======================================");
            }

            if (response != null) {
                Object codeObj = response.get("Code");
                Object messageObj = response.get("Message");
                String responseCode = codeObj != null ? codeObj.toString() : null;
                String responseMessage = messageObj != null ? messageObj.toString() : null;
                
                logger.info("短信发送响应 - Code: " + responseCode + ", Message: " + responseMessage);
                
                if ("OK".equals(responseCode)) {
                    String generatedCode = null;
                    String accessCode = null;
                    
                    logger.info("========== 开始提取验证码和 AccessCode ==========");
                    
                    Object accessCodeObj = response.get("AccessCode");
                    if (accessCodeObj == null) {
                        accessCodeObj = response.get("accessCode");
                    }
                    if (accessCodeObj == null) {
                        accessCodeObj = response.get("AccessCode");
                    }
                    if (accessCodeObj != null) {
                        accessCode = accessCodeObj.toString();
                        logger.info("从响应根级别找到 AccessCode: " + accessCode);
                    }
                    
                    Object modelObj = response.get("Model");
                    if (modelObj != null) {
                        logger.info("找到 Model 字段，类型: " + modelObj.getClass().getName());
                        logger.info("Model 内容: " + modelObj.toString());
                        
                        if (modelObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> model = (Map<String, Object>) modelObj;
                            logger.info("Model Map 中的所有键: " + model.keySet());
                            
                            Object codeObj2 = model.get("VerifyCode");
                            if (codeObj2 == null) {
                                codeObj2 = model.get("verifyCode");
                            }
                            if (codeObj2 == null) {
                                codeObj2 = model.get("Code");
                            }
                            if (codeObj2 != null) {
                                generatedCode = codeObj2.toString();
                                logger.info("从 Model 中找到验证码: " + generatedCode);
                            }
                            
                            if (accessCode == null) {
                                Object accessCodeObj2 = model.get("AccessCode");
                                if (accessCodeObj2 == null) {
                                    accessCodeObj2 = model.get("accessCode");
                                }
                                if (accessCodeObj2 == null) {
                                    accessCodeObj2 = model.get("AccessCode");
                                }
                                if (accessCodeObj2 != null) {
                                    accessCode = accessCodeObj2.toString();
                                    logger.info("从 Model 中找到 AccessCode: " + accessCode);
                                }
                            }
                        } else if (modelObj instanceof String) {
                            logger.info("Model 是字符串类型，尝试解析: " + modelObj);
                            Map<String, Object> modelMap = parseJsonResponse(modelObj.toString());
                            if (modelMap != null) {
                                logger.info("解析后的 Model Map 键: " + modelMap.keySet());
                                Object codeObj2 = modelMap.get("VerifyCode");
                                if (codeObj2 != null) {
                                    generatedCode = codeObj2.toString();
                                }
                                if (accessCode == null) {
                                    Object accessCodeObj2 = modelMap.get("AccessCode");
                                    if (accessCodeObj2 != null) {
                                        accessCode = accessCodeObj2.toString();
                                        logger.info("从解析后的 Model 中找到 AccessCode: " + accessCode);
                                    }
                                }
                            }
                        }
                    } else {
                        logger.warning("响应中没有 Model 字段");
                    }
                    
                    String bizId = null;
                    if (modelObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> model = (Map<String, Object>) modelObj;
                        Object bizIdObj = model.get("BizId");
                        if (bizIdObj != null) {
                            bizId = bizIdObj.toString();
                            logger.info("从 Model 中找到 BizId: " + bizId);
                        }
                    }
                    
                    logger.info("========== 提取结果 ==========");
                    logger.info("验证码: " + (generatedCode != null ? generatedCode : "未找到"));
                    logger.info("AccessCode: " + (accessCode != null ? accessCode : "未找到"));
                    logger.info("BizId: " + (bizId != null ? bizId : "未找到"));
                    logger.info("==============================");
                    
                    logger.info("短信验证码发送成功，手机号: " + phone);
                    if (generatedCode != null) {
                        logger.info("系统生成的验证码: " + generatedCode);
                    }
                    
                    if (accessCode != null) {
                        logger.info("AccessCode: " + accessCode);
                    } else if (bizId != null) {
                        logger.info("未获取到 AccessCode，将使用 BizId 作为 AccessCode: " + bizId);
                        accessCode = bizId;
                    } else {
                        logger.warning("未获取到 AccessCode 和 BizId，验证时可能会失败");
                    }
                    
                    if (accessCode == null) {
                        logger.severe("严重错误：AccessCode 和 BizId 都为空，无法进行验证");
                        return null;
                    }
                    logger.info("最终返回 - 验证码: " + generatedCode + ", AccessCode/BizId: " + accessCode);
                    return new String[]{generatedCode, accessCode};
                } else {
                    String detailedMessage = getErrorMessage(responseCode, responseMessage);
                    logger.warning("短信发送失败，响应码: " + responseCode + ", 消息: " + responseMessage);
                    logger.warning("详细错误信息: " + detailedMessage);
                    
                    if ("InternalError".equals(responseCode)) {
                        logger.severe("========== InternalError 排查建议 ==========");
                        logger.severe("1. 检查模板参数名称是否匹配：");
                        logger.severe("   - 当前使用的参数名: " + paramName);
                        logger.severe("   - 模板参数格式: {\"" + paramName + "\":\"##code##\",\"min\":\"5\"}");
                        logger.severe("   - 请确认模板中的参数名称是否与配置一致");
                        logger.severe("2. 检查签名和模板配置：");
                        logger.severe("   - 签名名称: " + signName);
                        logger.severe("   - 模板代码: " + templateCode);
                        logger.severe("   - 请确认签名和模板已在阿里云控制台审核通过");
                        logger.severe("3. 检查模板参数格式：");
                        logger.severe("   - 模板参数 JSON: " + templateParam);
                        logger.severe("   - 确保使用 ##code## 占位符（系统生成验证码）");
                        logger.severe("   - 确保 min 参数是字符串格式");
                        logger.severe("4. 如果模板参数名不是 'code'，请在 sms.properties 中配置：");
                        logger.severe("   - sms.templateParamName=你的参数名");
                        logger.severe("==========================================");
                    }
                    
                    if ("isv.INVALID_PARAMETERS".equals(responseCode)) {
                        logger.severe("========== 参数错误调试信息 ==========");
                        logger.severe("手机号: " + phone);
                        logger.severe("签名名称: [" + signName + "] (长度: " + signName.length() + ")");
                        logger.severe("模板代码: [" + templateCode + "]");
                        logger.severe("模板参数: " + templateParam);
                        logger.severe("国家代码: 86");
                        logger.severe("请检查：");
                        logger.severe("1. 签名名称是否与阿里云控制台中的签名完全一致（包括空格、大小写）");
                        logger.severe("2. 模板代码是否正确");
                        logger.severe("3. 模板参数格式是否符合模板要求");
                        logger.severe("4. AccessKeyId 和 AccessKeySecret 是否正确");
                        logger.severe("======================================");
                    }
                    return null;
                }
            } else {
                logger.warning("短信发送响应体为空");
                return null;
            }
        } catch (Exception e) {
            logger.severe("发送短信验证码时发生异常: " + e.getMessage());
            e.printStackTrace();
            StackTraceElement[] stackTrace = e.getStackTrace();
            for (StackTraceElement element : stackTrace) {
                logger.severe("  at " + element.toString());
            }
            return null;
        }
    }

    @Override
    public boolean sendVerificationCode(String phone, String code) {
        try {
            if (smsConfig.getAccessKeyId() == null || smsConfig.getAccessKeyId().isEmpty() ||
                smsConfig.getAccessKeySecret() == null || smsConfig.getAccessKeySecret().isEmpty()) {
                logger.severe("短信服务配置错误：AccessKeyId 或 AccessKeySecret 为空");
                return false;
            }
            
            if (smsConfig.getSignName() == null || smsConfig.getSignName().trim().isEmpty()) {
                logger.severe("短信服务配置错误：签名名称为空");
                return false;
            }
            
            if (smsConfig.getTemplateCode() == null || smsConfig.getTemplateCode().trim().isEmpty()) {
                logger.severe("短信服务配置错误：模板代码为空");
                return false;
            }

            logger.info("========== 短信服务配置信息 ==========");
            logger.info("AccessKeyId: " + (smsConfig.getAccessKeyId().length() > 10 ? 
                smsConfig.getAccessKeyId().substring(0, 10) + "..." : smsConfig.getAccessKeyId()));
            logger.info("签名名称: " + smsConfig.getSignName() + " (长度: " + smsConfig.getSignName().length() + ")");
            logger.info("模板代码: " + smsConfig.getTemplateCode());
            logger.info("端点: " + smsConfig.getEndpoint());
            logger.info("======================================");
            
            logger.warning("使用已废弃的 sendVerificationCode 方法，建议使用 sendVerificationCodeWithDypns");
            String[] result = sendVerificationCodeWithDypns(phone);
            if (result != null && result[0] != null) {
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.severe("发送短信验证码时发生异常: " + e.getMessage());
            e.printStackTrace();
            StackTraceElement[] stackTrace = e.getStackTrace();
            for (StackTraceElement element : stackTrace) {
                logger.severe("  at " + element.toString());
            }
            return false;
        }
    }

    @Override
    public String generateVerificationCode(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    @Override
    public boolean verifyCodeWithDypns(String phone, String code) {
        try {
            if (smsConfig.getAccessKeyId() == null || smsConfig.getAccessKeyId().isEmpty() ||
                smsConfig.getAccessKeySecret() == null || smsConfig.getAccessKeySecret().isEmpty()) {
                logger.severe("短信服务配置错误：AccessKeyId 或 AccessKeySecret 为空");
                return false;
            }
            
            if (smsConfig.getSignName() == null || smsConfig.getSignName().trim().isEmpty()) {
                logger.severe("短信服务配置错误：签名名称为空");
                return false;
            }
            
            if (smsConfig.getTemplateCode() == null || smsConfig.getTemplateCode().trim().isEmpty()) {
                logger.severe("短信服务配置错误：模板代码为空");
                return false;
            }
            
            if (phone == null || phone.trim().isEmpty() || code == null || code.trim().isEmpty()) {
                logger.warning("验证码验证失败：手机号或验证码为空");
                return false;
            }
            
            logger.info("开始验证验证码，手机号: " + phone);
            logger.info("使用 dypns（号码认证服务）API 验证");
            logger.info("注意：CheckSmsVerifyCode 接口不需要 AccessCode 参数，只需要手机号和验证码");

            String endpoint = "https://dypnsapi.aliyuncs.com";
            String action = "CheckSmsVerifyCode";
            String version = "2017-05-25";
            
            Map<String, String> params = new HashMap<>();
            params.put("Action", action);
            params.put("Version", version);
            params.put("AccessKeyId", smsConfig.getAccessKeyId());
            params.put("Format", "JSON");
            params.put("SignatureMethod", "HMAC-SHA1");
            params.put("SignatureVersion", "1.0");
            params.put("SignatureNonce", UUID.randomUUID().toString());
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String timestamp = sdf.format(new Date());
            params.put("Timestamp", timestamp);
            
            params.put("PhoneNumber", phone);
            params.put("VerifyCode", code);
            params.put("CountryCode", "86");
            
            logger.info("验证请求参数 - 手机号: " + phone + ", 验证码: " + code);
            logger.info("使用 CheckSmsVerifyCode 接口（不需要 AccessCode）");
            
            String signature = generateSignature(params, smsConfig.getAccessKeySecret(), "POST");
            params.put("Signature", signature);
            
            String responseStr = sendHttpRequest(endpoint, params);
            logger.info("验证 API 响应: " + responseStr);
            
            Map<String, Object> response = parseJsonResponse(responseStr);
            
            if (response != null) {
                Object codeObj = response.get("Code");
                Object messageObj = response.get("Message");
                String responseCode = codeObj != null ? codeObj.toString() : null;
                String responseMessage = messageObj != null ? messageObj.toString() : null;
                
                logger.info("验证响应 - Code: " + responseCode + ", Message: " + responseMessage);
                
                if ("OK".equals(responseCode)) {
                    logger.info("验证码验证成功，手机号: " + phone);
                    return true;
                } else {
                    logger.warning("验证码验证失败，响应码: " + responseCode + ", 消息: " + responseMessage);
                    return false;
                }
            } else {
                logger.warning("验证响应体为空");
                return false;
            }
        } catch (Exception e) {
            logger.severe("验证验证码时发生异常: " + e.getMessage());
            e.printStackTrace();
            StackTraceElement[] stackTrace = e.getStackTrace();
            for (StackTraceElement element : stackTrace) {
                logger.severe("  at " + element.toString());
            }
            return false;
        }
    }

    private String generateSignature(Map<String, String> params, String accessKeySecret, String method) throws Exception {
        List<String> sortedKeys = new ArrayList<>(params.keySet());
        Collections.sort(sortedKeys);
        
        StringBuilder stringToSign = new StringBuilder();
        stringToSign.append(method).append("&");
        stringToSign.append(percentEncode("/")).append("&");
        
        StringBuilder queryString = new StringBuilder();
        for (String key : sortedKeys) {
            if (!"Signature".equals(key)) {
                if (queryString.length() > 0) {
                    queryString.append("&");
                }
                queryString.append(percentEncode(key)).append("=").append(percentEncode(params.get(key)));
            }
        }
        stringToSign.append(percentEncode(queryString.toString()));
        
        String secret = accessKeySecret + "&";
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
        javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
        mac.init(secretKeySpec);
        byte[] signData = mac.doFinal(stringToSign.toString().getBytes(StandardCharsets.UTF_8));
        
        return Base64.getEncoder().encodeToString(signData);
    }
    
    private String percentEncode(String value) throws Exception {
        if (value == null) {
            return "";
        }
        return URLEncoder.encode(value, "UTF-8")
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }
    
    private String sendHttpRequest(String urlStr, Map<String, String> params) throws Exception {
        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (queryString.length() > 0) {
                queryString.append("&");
            }
            queryString.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                      .append("=")
                      .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = queryString.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
            }
        } else {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                throw new Exception("HTTP 错误 " + responseCode + ": " + response.toString());
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonResponse(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return null;
        }
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonStr).getAsJsonObject();
            
            Map<String, Object> result = new HashMap<>();
            for (String key : jsonObject.keySet()) {
                var element = jsonObject.get(key);
                if (element.isJsonPrimitive()) {
                    result.put(key, element.getAsString());
                } else if (element.isJsonObject()) {
                    Map<String, Object> nestedMap = new HashMap<>();
                    JsonObject nestedObj = element.getAsJsonObject();
                    for (String nestedKey : nestedObj.keySet()) {
                        var nestedElement = nestedObj.get(nestedKey);
                        if (nestedElement.isJsonPrimitive()) {
                            nestedMap.put(nestedKey, nestedElement.getAsString());
                        } else {
                            nestedMap.put(nestedKey, nestedElement.toString());
                        }
                    }
                    result.put(key, nestedMap);
                } else {
                    result.put(key, element.toString());
                }
            }
            return result;
        } catch (Exception e) {
            logger.warning("解析 JSON 响应失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private String getErrorMessage(String errorCode, String errorMessage) {
        if (errorCode == null) {
            return "未知错误";
        }
        
        switch (errorCode) {
            case "InternalError":
                return "服务器内部错误。可能原因：1) 模板参数名称不匹配（请检查 sms.templateParamName 配置）；2) 签名或模板配置问题；3) 参数格式错误。请查看日志获取详细排查信息。";
            case "isv.SMS_TEMPLATE_ILLEGAL":
                return "短信模板代码不存在或未审核通过。请检查 sms.properties 中的 sms.templateCode 配置，确保模板代码与阿里云控制台中的模板代码一致。";
            case "isv.INVALID_SIGN_NAME":
                return "短信签名不存在或未审核通过。请检查 sms.properties 中的 sms.signName 配置，确保签名与阿里云控制台中的签名完全一致（包括空格、大小写、特殊字符）。";
            case "isv.INVALID_PARAMETERS":
                return "参数格式不正确。请检查：1) 签名名称是否与阿里云控制台完全一致（去除首尾空格）；2) 模板代码是否正确；3) 模板参数格式是否符合模板要求；4) AccessKeyId 和 AccessKeySecret 是否正确。";
            case "isv.INVALID_ACCESS_KEY_ID":
                return "AccessKeyId 无效。请检查 sms.properties 中的 sms.accessKeyId 配置。";
            case "isv.INVALID_ACCESS_KEY_SECRET":
                return "AccessKeySecret 无效。请检查 sms.properties 中的 sms.accessKeySecret 配置。";
            case "isv.INSUFFICIENT_BALANCE":
                return "账户余额不足，请充值后重试。";
            case "isv.BUSINESS_LIMIT_CONTROL":
                return "业务限流，请稍后再试。";
            default:
                return "错误码: " + errorCode + ", 错误信息: " + errorMessage;
        }
    }
}
