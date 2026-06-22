package com.app.mq;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ChatMessage implements Serializable {

    private Integer fromUserId;

    private String fromUsername;

    private String fromAvatar;

    private Integer toUserId;

    private String content;

    private Integer messageId;

    private Date createTime;
}
