package com.app.mq.consumer;

import com.app.config.RabbitMQConfig;
import com.app.mq.NotificationMessage;
import com.app.service.WNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationConsumer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationConsumer.class);

    @Autowired
    private WNotificationService notificationService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotification(NotificationMessage message) {
        try {
            logger.info("收到通知消息: userId={}, type={}, content={}", 
                    message.getUserId(), message.getType(), message.getContent());
            
            notificationService.createNotification(message);
            
        } catch (Exception e) {
            logger.error("处理通知消息失败", e);
        }
    }
}
