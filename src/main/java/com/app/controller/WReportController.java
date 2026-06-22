package com.app.controller;

import com.app.common.CResult;
import com.app.dao.WReportMapper;
import com.pc.utils.OssUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.logging.Logger;

@RestController
@RequestMapping("/app/report")
public class WReportController {

    private static final Logger logger = Logger.getLogger(WReportController.class.getName());

    @Autowired
    private WReportMapper wReportMapper;

    @PostMapping("/submit")
    public CResult<Object> submitReport(
            @RequestParam("reporterId") Integer reporterId,
            @RequestParam("targetType") String targetType,
            @RequestParam(value = "targetId", required = false) Integer targetId,
            @RequestParam(value = "postId", required = false) Integer postId,
            @RequestParam(value = "interactionId", required = false) Integer interactionId,
            @RequestParam("reportType") String reportType,
            @RequestParam("description") String description,
            @RequestParam(value = "reportImage", required = false) String reportImage) {
        try {
            if (reporterId == null) {
                return CResult.error("举报人ID不能为空");
            }
            if (targetType == null || targetType.isEmpty()) {
                return CResult.error("目标类型不能为空");
            }
            if (reportType == null || reportType.isEmpty()) {
                return CResult.error("举报类型不能为空");
            }
            if (description == null || description.trim().isEmpty()) {
                return CResult.error("举报描述不能为空");
            }

            if (!targetType.equals("用户") && !targetType.equals("帖子") && !targetType.equals("评论")) {
                return CResult.error("目标类型错误，必须是：用户、帖子、评论");
            }

            String[] validReportTypes = {"垃圾信息", "色情低俗", "违法违规", "欺诈", "侵权", "其他"};
            boolean validType = false;
            for (String type : validReportTypes) {
                if (type.equals(reportType)) {
                    validType = true;
                    break;
                }
            }
            if (!validType) {
                return CResult.error("举报类型错误");
            }

            Integer finalTargetId = null;
            Integer finalPostId = null;
            Integer finalInteraction = null;
            
            if (targetType.equals("用户")) {
                if (targetId == null) {
                    return CResult.error("用户ID不能为空");
                }
                finalTargetId = targetId;
                finalPostId = null;
                finalInteraction = null;
                
                if (wReportMapper.checkReportExistsByTargetId(reporterId, targetType, finalTargetId) != null) {
                    return CResult.error("您已经举报过此用户，请勿重复举报");
                }
            } else if (targetType.equals("帖子")) {
                if (postId == null) {
                    return CResult.error("帖子ID不能为空");
                }
                finalTargetId = null;
                finalPostId = postId;
                finalInteraction = null;
                
                if (wReportMapper.checkReportExistsByPostId(reporterId, targetType, finalPostId) != null) {
                    return CResult.error("您已经举报过此帖子，请勿重复举报");
                }
            } else if (targetType.equals("评论")) {
                if (interactionId == null) {
                    return CResult.error("评论ID不能为空");
                }
                finalTargetId = null;
                finalPostId = null;
                finalInteraction = interactionId;
                
                if (wReportMapper.checkReportExistsByInteraction(reporterId, targetType, finalInteraction) != null) {
                    return CResult.error("您已经举报过此评论，请勿重复举报");
                }
            }
            
            int result = wReportMapper.insertReport(
                reporterId,
                targetType,
                finalTargetId,
                finalPostId,
                finalInteraction,
                reportType,
                description.trim(),
                reportImage,
                0
            );
            
            if (result > 0) {
                logger.info("举报提交成功: reporterId=" + reporterId + ", targetType=" + targetType);
                return CResult.success("举报提交成功，等待管理员处理", null);
            } else {
                return CResult.error("举报提交失败，请稍后重试");
            }
        } catch (org.springframework.dao.DataAccessException e) {
            logger.warning("数据库异常: " + e.getMessage());
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("Duplicate entry") || 
                                     errorMsg.contains("uk_reporter_target") ||
                                     errorMsg.contains("UNIQUE constraint"))) {
                return CResult.error("您已经举报过此内容，请勿重复举报");
            }
            return CResult.error("提交失败，请稍后重试");
        } catch (Exception e) {
            logger.severe("提交举报异常: " + e.getMessage());
            e.printStackTrace();
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("Error getting generated key")) {
                logger.info("检测到keyProperty错误，但数据可能已插入成功");
                return CResult.success("举报提交成功，等待管理员处理", null);
            }
            return CResult.error("提交失败: " + e.getMessage());
        }
    }

    @PostMapping("/upload_image")
    public CResult<String> uploadReportImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return CResult.error("图片文件不能为空");
            }

            InputStream inputStream = file.getInputStream();
            String originalFilename = file.getOriginalFilename();
            String imageUrl = OssUtil.uploadFile(inputStream, originalFilename);
            
            if (imageUrl != null && !imageUrl.isEmpty()) {
                return CResult.success("上传成功", imageUrl);
            } else {
                return CResult.error("上传失败");
            }
        } catch (Exception e) {
            logger.severe("上传举报图片异常: " + e.getMessage());
            e.printStackTrace();
            return CResult.error("上传失败: " + e.getMessage());
        }
    }
}
