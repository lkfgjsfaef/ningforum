package com.app.mq.producer;

import com.app.config.RabbitMQConfig;
import com.app.mq.CacheInvalidationMessage;
import com.app.mq.ChatMessage;
import com.app.mq.NotificationMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendNotification(NotificationMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                message
        );
    }

    public void sendChatMessage(ChatMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.MESSAGE_EXCHANGE,
                RabbitMQConfig.MESSAGE_ROUTING_KEY,
                message
        );
    }

    public void sendCacheInvalidation(CacheInvalidationMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CACHE_EXCHANGE,
                RabbitMQConfig.CACHE_ROUTING_KEY,
                message
        );
    }
}
