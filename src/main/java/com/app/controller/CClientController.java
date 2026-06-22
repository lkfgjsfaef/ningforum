package com.app.controller;

import com.app.common.CResult;
import com.app.common.VerificationCodeManager;
import com.app.pojo.CUser;
import com.app.service.CUserService;
import com.app.service.SmsService;
import com.pc.utils.OssUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/cuser")
public class CClientController {
    
    private static final Logger logger = Logger.getLogger(CClientController.class.getName());

    @Autowired
    private CUserService cUserService;
    
    @Autowired
    private SmsService smsService;
    
    @Autowired
    private VerificationCodeManager verificationCodeManager;
    
    @Autowired
    private com.app.service.WCircleService wCircleService;

    @PostMapping("/login")
    public CResult<CUser> login(@RequestParam("username") String username,
                                 @RequestParam("password") String password) {
        try {
            CUser user = cUserService.login(username, password);
            if (user != null) {
                return CResult.success("登录成功", user);
            }
            return CResult.error("账号或密码错误");
        } catch (RuntimeException e) {
            if ("BANNED_USER".equals(e.getMessage())) {
                return CResult.error("你已被系统封禁，当前禁止登录小宁论坛");
            }
            throw e;
        }
    }

    @PostMapping("/register")
    public CResult<Object> register(@RequestParam("username") String username,
                                    @RequestParam("realName") String realName,
                                    @RequestParam("phone") String phone,
                                    @RequestParam("password") String password) {
        CUser user = new CUser();
        user.setUsername(username);
        user.setRealName(realName);
        user.setPhone(phone);
        user.setPassword(password);

        if (cUserService.register(user)) {
            return CResult.success("注册成功", null);
        }
        return CResult.error("注册失败，用户名或手机号已存在");
    }

    @PostMapping("/update")
    public CResult<CUser> update(@RequestParam(value = "userId", required = true) Integer userId,
                                 @RequestParam(value = "username", required = false) String username,
                                 @RequestParam(value = "realName", required = false) String realName,
                                 @RequestParam(value = "phone", required = false) String phone,
                                 @RequestParam(value = "email", required = false) String email,
                                 @RequestParam(value = "gender", required = false) Integer gender,
                                 @RequestParam(value = "signature", required = false) String signature,
                                 @RequestParam(value = "address", required = false) String address,
                                 @RequestParam(value = "password", required = false) String password,
                                 @RequestParam(value = "avatar", required = false) String avatar) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            
            CUser user = new CUser();
            user.setUserId(userId);
            if (username != null && !username.trim().isEmpty()) {
                user.setUsername(username.trim());
            }
            if (realName != null && !realName.trim().isEmpty()) {
                user.setRealName(realName.trim());
            }
            if (phone != null && !phone.trim().isEmpty()) {
                user.setPhone(phone.trim());
            }
            if (email != null && !email.trim().isEmpty()) {
                user.setEmail(email.trim());
            }
            if (gender != null) {
                user.setGender(gender);
            }
            if (signature != null) {
                user.setSignature(signature.trim());
            }
            if (address != null && !address.trim().isEmpty()) {
                user.setAddress(address.trim());
            }
            if (password != null && !password.trim().isEmpty()) {
                user.setPassword(password.trim());
            }
            if (avatar != null && !avatar.trim().isEmpty()) {
                user.setAvatar(avatar.trim());
            }
            
            if (cUserService.updateInfo(user)) {
                CUser updatedUser = cUserService.getUserInfo(userId);
                if (updatedUser != null) {
                    return CResult.success("更新成功", updatedUser);
                }
                return CResult.error("更新成功但获取用户信息失败");
            }
            return CResult.error("更新失败");
        } catch (Exception e) {
            logger.severe("更新用户信息异常: " + e.getMessage());
            e.printStackTrace();
            return CResult.error("更新失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/upload_avatar")
    public CResult<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                        @RequestParam("userId") Integer userId) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            
            if (file == null || file.isEmpty()) {
                return CResult.error("请选择图片");
            }
            
            long maxSize = 10 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return CResult.error("图片大小不能超过10MB");
            }
            
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                return CResult.error("文件名无效");
            }
            
            String extension = "";
            int lastDotIndex = originalFilename.lastIndexOf('.');
            if (lastDotIndex > 0) {
                extension = originalFilename.substring(lastDotIndex).toLowerCase();
            }
            
            String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"};
            boolean isValidExtension = false;
            for (String ext : allowedExtensions) {
                if (extension.equals(ext)) {
                    isValidExtension = true;
                    break;
                }
            }
            
            if (!isValidExtension) {
                return CResult.error("只支持JPG、PNG、GIF、BMP、WEBP格式的图片");
            }
            
            InputStream inputStream = file.getInputStream();
            String avatarUrl = OssUtil.uploadAvatar(inputStream, originalFilename);
            inputStream.close();
            
            CUser user = new CUser();
            user.setUserId(userId);
            user.setAvatar(avatarUrl);
            if (cUserService.updateInfo(user)) {
                return CResult.success("头像上传成功", avatarUrl);
            } else {
                return CResult.error("头像上传成功但更新用户信息失败");
            }
            
        } catch (Exception e) {
            logger.severe("上传头像异常: " + e.getMessage());
            e.printStackTrace();
            return CResult.error("上传失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/sendCode")
    public CResult<Object> sendCode(@RequestParam("phone") String phone) {
        try {
            if (phone == null || phone.trim().isEmpty()) {
                return CResult.error("手机号不能为空");
            }
            
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                return CResult.error("手机号格式不正确");
            }
            
            String[] result = smsService.sendVerificationCodeWithDypns(phone);
            logger.info("发送验证码返回结果: " + (result != null ? "result[0]=" + result[0] + ", result[1]=" + result[1] : "null"));
            
            if (result != null && result.length >= 2) {
                String generatedCode = result[0];
                String accessCode = result[1];
                
                if (accessCode != null && !accessCode.isEmpty()) {
                    verificationCodeManager.storeCodeWithAccessCode(phone, generatedCode, accessCode);
                    logger.info("验证码已存储，手机号: " + phone + ", AccessCode/BizId: " + accessCode);
                    return CResult.success("验证码发送成功", null);
                } else {
                    logger.warning("AccessCode/BizId 为空，无法存储");
                }
            } else {
                logger.warning("发送验证码返回结果异常: " + (result != null ? "长度=" + result.length : "null"));
            }
            return CResult.error("验证码发送失败，请检查服务器日志或联系管理员");
        } catch (Exception e) {
            e.printStackTrace();
            return CResult.error("发送验证码时发生异常: " + e.getMessage());
        }
    }
    
    @PostMapping("/loginByCode")
    public CResult<CUser> loginByCode(@RequestParam("phone") String phone,
                                     @RequestParam("code") String code) {
        try {
            if (phone == null || phone.trim().isEmpty()) {
                return CResult.error("手机号不能为空");
            }
            
            if (code == null || code.trim().isEmpty()) {
                return CResult.error("验证码不能为空");
            }
            
            boolean isValid = smsService.verifyCodeWithDypns(phone, code);
            if (isValid) {
                try {
                    CUser user = cUserService.findByPhone(phone);
                    if (user != null) {
                        user.setPassword(null);
                        return CResult.success("登录成功", user);
                    }
                    return CResult.error("用户不存在");
                } catch (RuntimeException e) {
                    if ("BANNED_USER".equals(e.getMessage())) {
                        return CResult.error("你已被系统封禁，当前禁止登录小宁论坛");
                    }
                    throw e;
                }
            }
            return CResult.error("验证码错误或已过期");
        } catch (Exception e) {
            e.printStackTrace();
            return CResult.error("验证验证码时发生异常: " + e.getMessage());
        }
    }
    
    @GetMapping("/userInfo")
    public CResult<CUser> getUserInfo(@RequestParam("userId") Integer userId) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            CUser user = cUserService.getUserInfo(userId);
            if (user != null) {
                return CResult.success("获取成功", user);
            }
            return CResult.error("用户不存在");
        } catch (Exception e) {
            e.printStackTrace();
            return CResult.error("获取用户信息时发生异常: " + e.getMessage());
        }
    }
    
    @GetMapping("/userInfoByUsername")
    public CResult<CUser> getUserInfoByUsername(@RequestParam("username") String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return CResult.error("用户名不能为空");
            }
            CUser user = cUserService.findByUsername(username.trim());
            if (user != null) {
                return CResult.success("获取成功", user);
            }
            return CResult.error("用户不存在");
        } catch (Exception e) {
            e.printStackTrace();
            return CResult.error("获取用户信息时发生异常: " + e.getMessage());
        }
    }
    
    @GetMapping("/relation_list")
    public CResult<List<CUser>> getRelationList(@RequestParam("userId") Integer userId,
                                                @RequestParam("type") Integer type) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            if (type == null || (type != 0 && type != 1)) {
                return CResult.error("类型参数错误，0=关注列表，1=粉丝列表");
            }
            
            List<CUser> users = cUserService.getRelationList(userId, type);
            return CResult.success("查询成功", users);
        } catch (Exception e) {
            logger.severe("获取关注/粉丝列表异常: " + e.getMessage());
            e.printStackTrace();
            return CResult.error("查询失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/follow_action")
    public CResult<Object> followAction(@RequestParam("userId") Integer userId,
                                       @RequestParam("targetUserId") Integer targetUserId,
                                       @RequestParam("actionType") Integer actionType) {
        try {
            if (userId == null || targetUserId == null) {
                return CResult.error("用户ID不能为空");
            }
            if (actionType == null || actionType < 0 || actionType > 2) {
                return CResult.error("操作类型错误，0=关注，1=取消关注，2=移除粉丝");
            }
            
            boolean success = cUserService.followAction(userId, targetUserId, actionType);
            if (success) {
                String message = actionType == 0 ? "关注成功" : (actionType == 1 ? "取消关注成功" : "移除粉丝成功");
                return CResult.success(message, null);
            }
            return CResult.error("操作失败");
        } catch (Exception e) {
            logger.severe("关注操作异常: " + e.getMessage());
            e.printStackTrace();
            return CResult.error("操作失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/user_stats")
    public CResult<Map<String, Integer>> getUserStats(@RequestParam("userId") Integer userId) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            
            Map<String, Integer> stats = cUserService.getUserStats(userId);
            return CResult.success("查询成功", stats);
        } catch (Exception e) {
            logger.severe("获取用户统计异常: " + e.getMessage());
            e.printStackTrace();
            return CResult.error("查询失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/check_follow")
    public CResult<Boolean> checkFollow(@RequestParam("userId") Integer userId,
                                       @RequestParam("targetUserId") Integer targetUserId) {
        try {
            if (userId == null || targetUserId == null) {
                return CResult.error("用户ID不能为空");
            }
            
            boolean exists = cUserService.checkFollowExists(userId, targetUserId);
            return CResult.success("查询成功", exists);
        } catch (Exception e) {
            logger.severe("检查关注状态异常: " + e.getMessage());
            e.printStackTrace();
            return CResult.error("查询失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/block_user")
    public CResult<Object> blockUser(@RequestParam("userId") Integer userId,
                                    @RequestParam("targetUserId") Integer targetUserId) {
        try {
            if (userId == null || targetUserId == null) {
                return CResult.error("用户ID不能为空");
            }
            if (userId.equals(targetUserId)) {
                return CResult.error("不能拉黑自己");
            }
            
            boolean success = cUserService.blockUser(userId, targetUserId);
            if (success) {
                return CResult.success("拉黑成功", null);
            }
            return CResult.error("拉黑失败");
        } catch (Exception e) {
            logger.severe("拉黑用户异常: " + e.getMessage());
            e.printStackTrace();
            return CResult.error("拉黑失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/unblock_user")
    public CResult<Object> unblockUser(@RequestParam("userId") Integer userId,
                                      @RequestParam("targetUserId") Integer targetUserId) {
        try {
            if (userId == null || targetUserId == null) {
                return CResult.error("用户ID不能为空");
            }
            
            boolean success = cUserService.unblockUser(userId, targetUserId);
            if (success) {
                return CResult.success("取消拉黑成功", null);
            }
            return CResult.error("取消拉黑失败");
        } catch (Exception e) {
            logger.severe("取消拉黑异常: " + e.getMessage());
            e.printStackTrace();
            return CResult.error("取消拉黑失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/blacklist")
    public CResult<List<CUser>> getBlacklist(@RequestParam("userId") Integer userId) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            
            List<CUser> blacklist = cUserService.getBlacklist(userId);
            return CResult.success("查询成功", blacklist);
        } catch (Exception e) {
            logger.severe("获取黑名单异常: " + e.getMessage());
            e.printStackTrace();
            return CResult.error("查询失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/check_block")
    public CResult<Boolean> checkBlock(@RequestParam("userId") Integer userId,
                               @RequestParam("targetUserId") Integer targetUserId) {
        try {
            if (userId == null || targetUserId == null) {
                return CResult.error("用户ID不能为空");
            }
            
            boolean exists = cUserService.checkBlockExists(userId, targetUserId);
            return CResult.success("查询成功", exists);
        } catch (Exception e) {
            logger.severe("检查拉黑状态异常: " + e.getMessage());
            e.printStackTrace();
            return CResult.error("查询失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/my_watches")
    public CResult<List<Map<String, Object>>> getMyWatches(@RequestParam("userId") Integer userId) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            
            Map<String, Object> result = wCircleService.getFavoritePosts(userId, 1, 100);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> posts = (List<Map<String, Object>>) result.get("posts");
            if (posts == null) {
                posts = new java.util.ArrayList<>();
            }
            
            List<Map<String, Object>> cPostList = new java.util.ArrayList<>();
            for (Map<String, Object> post : posts) {
                Map<String, Object> cPost = new java.util.HashMap<>();
                cPost.put("postId", post.get("postId"));
                cPost.put("userId", post.get("userId"));
                cPost.put("title", post.get("title"));
                cPost.put("content", post.get("content"));
                
                Object imagesObj = post.get("images");
                if (imagesObj instanceof List) {
                    cPost.put("images", imagesObj);
                    if (((List<?>) imagesObj).size() > 0) {
                        cPost.put("image1", ((List<?>) imagesObj).get(0));
                    } else {
                        cPost.put("image1", null);
                    }
                } else {
                    cPost.put("images", new java.util.ArrayList<>());
                    Object image1Obj = post.get("image1");
                    cPost.put("image1", image1Obj);
                }
                
                cPost.put("createTime", post.get("time"));
                cPost.put("authorName", post.get("author"));
                cPost.put("authorAvatar", post.get("avatar"));
                cPost.put("views", post.get("views"));
                cPost.put("likes", post.get("likes"));
                cPost.put("comments", post.get("comments"));
                
                Object tagObj = post.get("tag");
                if (tagObj != null) {
                    cPost.put("tag", tagObj);
                }
                Object tagsObj = post.get("tags");
                if (tagsObj instanceof List) {
                    cPost.put("tags", tagsObj);
                }
                
                Object likedObj = post.get("liked");
                if (likedObj != null) {
                    cPost.put("liked", likedObj);
                }
                Object favoritedObj = post.get("favorited");
                if (favoritedObj != null) {
                    cPost.put("favorited", favoritedObj);
                }
                
                cPostList.add(cPost);
            }
            
            return CResult.success("查询成功", cPostList);
        } catch (Exception e) {
            logger.severe("获取收藏列表异常: " + e.getMessage());
            e.printStackTrace();
            return CResult.error("查询失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/my_posts")
    public CResult<List<Map<String, Object>>> getMyPosts(@RequestParam("userId") Integer userId) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            
            List<Map<String, Object>> posts = wCircleService.getUserPosts(userId, null);
            return CResult.success("查询成功", posts);
        } catch (Exception e) {
            logger.severe("获取用户帖子列表异常: " + e.getMessage());
            e.printStackTrace();
            return CResult.error("查询失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/my_comments")
    public CResult<List<Map<String, Object>>> getMyComments(@RequestParam("userId") Integer userId) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            
            List<Map<String, Object>> comments = wCircleService.getUserComments(userId);
            return CResult.success("查询成功", comments);
        } catch (Exception e) {
            logger.severe("获取用户评论列表异常: " + e.getMessage());
            e.printStackTrace();
            return CResult.error("查询失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/my_orders")
    public CResult<List<Map<String, Object>>> getMyOrders(@RequestParam("userId") Integer userId) {
        try {
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            
            List<Map<String, Object>> orders = wCircleService.getUserOrders(userId);
            return CResult.success("查询成功", orders);
        } catch (Exception e) {
            logger.severe("获取用户订单列表异常: " + e.getMessage());
            e.printStackTrace();
            return CResult.error("查询失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/delete_post")
    public CResult<Map<String, Object>> deletePost(@RequestParam("postId") Integer postId, 
                                                     @RequestParam("userId") Integer userId) {
        try {
            if (postId == null) {
                return CResult.error("动态ID不能为空");
            }
            if (userId == null) {
                return CResult.error("用户ID不能为空");
            }
            
            Map<String, Object> result = wCircleService.deletePost(postId, userId);
            Boolean success = (Boolean) result.get("success");
            String message = (String) result.get("message");
            
            if (success != null && success) {
                return CResult.success(message, result);
            } else {
                return CResult.error(message != null ? message : "删除失败");
            }
        } catch (Exception e) {
            logger.severe("删除动态异常: " + e.getMessage());
            e.printStackTrace();
            return CResult.error("删除失败: " + e.getMessage());
        }
    }
}
