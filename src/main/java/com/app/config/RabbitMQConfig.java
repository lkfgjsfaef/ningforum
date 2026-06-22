package com.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String MESSAGE_QUEUE = "message.queue";
    public static final String CACHE_QUEUE = "cache.queue";

    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String MESSAGE_EXCHANGE = "message.exchange";
    public static final String CACHE_EXCHANGE = "cache.exchange";

    public static final String NOTIFICATION_ROUTING_KEY = "notification.#";
    public static final String MESSAGE_ROUTING_KEY = "message.#";
    public static final String CACHE_ROUTING_KEY = "cache.#";

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Queue messageQueue() {
        return new Queue(MESSAGE_QUEUE, true);
    }

    @Bean
    public Queue cacheQueue() {
        return new Queue(CACHE_QUEUE, true);
    }

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange messageExchange() {
        return new TopicExchange(MESSAGE_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange cacheExchange() {
        return new TopicExchange(CACHE_EXCHANGE, true, false);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(notificationExchange())
                .with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding messageBinding() {
        return BindingBuilder.bind(messageQueue())
                .to(messageExchange())
                .with(MESSAGE_ROUTING_KEY);
    }

    @Bean
    public Binding cacheBinding() {
        return BindingBuilder.bind(cacheQueue())
                .to(cacheExchange())
                .with(CACHE_ROUTING_KEY);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public Jackson2JsonMessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }
}
