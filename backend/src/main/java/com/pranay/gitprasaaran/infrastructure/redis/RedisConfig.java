package com.pranay.gitprasaaran.infrastructure.redis;

import com.pranay.gitprasaaran.domain.document.Document;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Document> documentRedisTemplate(
            RedisConnectionFactory connectionFactory
    ) {
        RedisTemplate<String, Document> template = new RedisTemplate<>();

        JacksonJsonRedisSerializer<Document> documentSerializer =
                new JacksonJsonRedisSerializer<>(Document.class);

        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(StringRedisSerializer.UTF_8);
        template.setValueSerializer(documentSerializer);
        template.setHashKeySerializer(StringRedisSerializer.UTF_8);
        template.setHashValueSerializer(documentSerializer);
        template.afterPropertiesSet();

        return template;
    }
}