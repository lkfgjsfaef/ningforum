package com.app.mq.consumer;

import com.app.config.RabbitMQConfig;
import com.app.mq.ChatMessage;
import com.app.service.LMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageConsumer.class);

    @Autowired
    private LMessageService messageService;

    @RabbitListener(queues = RabbitMQConfig.MESSAGE_QUEUE)
    public void handleChatMessage(ChatMessage message) {
        try {
            logger.info("收到聊天消息: fromUserId={}, toUserId={}, content={}", 
                    message.getFromUserId(), message.getToUserId(), message.getContent());
            
            messageService.pushMessageToUser(message);
            
        } catch (Exception e) {
            logger.error("处理聊天消息失败", e);
        }
    }
}
