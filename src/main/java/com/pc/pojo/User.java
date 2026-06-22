package com.pc.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;
import java.util.List;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Data
/**
 * 用户实体类，对应数据库中的user表
 */
public class User {
    private Integer userId;
    private String username;
    private String realName;
    private String password;
    private String phone;
    private String email;
    private String avatar;
    private Integer gender;
    private String signature;
    private String address;
    private Integer role;
    private Integer status;
    private Integer warningCount;
    private Date createTime;
    private Date lastLoginTime;
    private com.pc.pojo.UserPermission permission; // 用户权限信息
    private List<UserBanHistory> banHistories; // 用户封禁历史列表
    private transient java.util.Map<String, String> activeBans; // 当前有效的封禁状态描述，transient字段不参与序列化
    private transient java.util.Map<String, Boolean> isPermanentBans; // 标记哪些权限是永久封禁，transient字段不参与序列化
    private Integer unreadCount;    // 未读消息数量


}
