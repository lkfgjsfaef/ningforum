package com.app.mq;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class NotificationMessage implements Serializable {

    private Integer userId;

    private Integer fromUserId;

    private String fromUsername;

    private String fromAvatar;

    private String type;

    private String content;

    private Integer postId;

    private Integer commentId;

    private Date createTime;
}
