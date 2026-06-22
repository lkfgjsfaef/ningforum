package com.app.mq.consumer;

import com.app.config.RabbitMQConfig;
import com.app.mq.CacheInvalidationMessage;
import com.app.utils.RedisCacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class CacheInvalidationConsumer {

    private static final Logger logger = LoggerFactory.getLogger(CacheInvalidationConsumer.class);

    @Autowired
    @Qualifier("appRedisCacheUtil")
    private RedisCacheUtil redisCacheUtil;

    @RabbitListener(queues = RabbitMQConfig.CACHE_QUEUE)
    public void handleCacheInvalidation(CacheInvalidationMessage message) {
        try {
            logger.info("收到缓存失效消息: type={}, key={}, pattern={}", 
                    message.getType(), message.getCacheKey(), message.getPattern());
            
            if ("key".equals(message.getType()) && message.getCacheKey() != null) {
                redisCacheUtil.delete(message.getCacheKey());
                logger.info("删除缓存key: {}", message.getCacheKey());
            } else if ("pattern".equals(message.getType()) && message.getPattern() != null) {
                redisCacheUtil.clearByPattern(message.getPattern());
                logger.info("删除缓存pattern: {}", message.getPattern());
            }
            
        } catch (Exception e) {
            logger.error("处理缓存失效消息失败", e);
        }
    }
}
