package com.pc.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 阿里云OSS工具类
 * 配置从 application.yml 中 aliyun.oss.* 读取
 */
@Component
public class OssUtil {

    private static String endpoint;
    private static String accessKeyId;
    private static String accessKeySecret;
    private static String bucketName;

    @Value("${aliyun.oss.endpoint:disabled}")
    public void setEndpoint(String val) { OssUtil.endpoint = val; }

    @Value("${aliyun.oss.access-key-id:disabled}")
    public void setAccessKeyId(String val) { OssUtil.accessKeyId = val; }

    @Value("${aliyun.oss.access-key-secret:disabled}")
    public void setAccessKeySecret(String val) { OssUtil.accessKeySecret = val; }

    @Value("${aliyun.oss.bucket-name:disabled}")
    public void setBucketName(String val) { OssUtil.bucketName = val; }

    private static void checkEnabled() {
        if ("disabled".equals(endpoint) || "disabled".equals(accessKeyId)
                || "disabled".equals(accessKeySecret) || "disabled".equals(bucketName)) {
            throw new RuntimeException("OSS未配置，请在application-dev.yml中填写 aliyun.oss.* 相关配置");
        }
    }

    public static String uploadFile(InputStream inputStream, String originalFilename) {
        checkEnabled();
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String dateDir = sdf.format(new Date());
            SimpleDateFormat timeSdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timeSdf.format(new Date());
            String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String objectName = "images/" + dateDir + "/" + timestamp + "_" + uuid + extension;

            ossClient.putObject(new PutObjectRequest(bucketName, objectName, inputStream));
            return "https://" + bucketName + "." + endpoint + "/" + objectName;
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.trim().isEmpty()) {
                errorMsg = "未知错误";
            }
            throw new RuntimeException("文件上传到OSS失败：" + errorMsg, e);
        } finally {
            if (ossClient != null) {
                try {
                    ossClient.shutdown();
                } catch (Exception ignored) {}
            }
        }
    }

    public static String uploadAvatar(InputStream inputStream, String originalFilename) {
        checkEnabled();
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
            } else {
                extension = ".jpg";
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String dateDir = sdf.format(new Date());
            SimpleDateFormat timeSdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = timeSdf.format(new Date());
            String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String objectName = "avatars/" + dateDir + "/" + timestamp + "_" + uuid + extension;

            ossClient.putObject(new PutObjectRequest(bucketName, objectName, inputStream));
            return "https://" + bucketName + "." + endpoint + "/" + objectName;
        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.trim().isEmpty()) {
                errorMsg = "未知错误";
            }
            throw new RuntimeException("头像上传到OSS失败：" + errorMsg, e);
        } finally {
            if (ossClient != null) {
                try {
                    ossClient.shutdown();
                } catch (Exception ignored) {}
            }
        }
    }

    public static void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        checkEnabled();
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

            String objectName = fileUrl;
            if (fileUrl.startsWith("https://")) {
                objectName = fileUrl.substring(fileUrl.indexOf("/", 8) + 1);
            } else if (fileUrl.startsWith("http://")) {
                objectName = fileUrl.substring(fileUrl.indexOf("/", 7) + 1);
            }

            ossClient.deleteObject(bucketName, objectName);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("删除OSS文件失败：" + e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}
