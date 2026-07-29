package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RedisConfig {

    @Bean
    public JedisPooled jedisPooled(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port,
            @Value("${spring.data.redis.password:}") String password) {

        if (password.isBlank()) {
            return new JedisPooled(host, port);
        }

        return new JedisPooled(host, port, Boolean.parseBoolean(password));
    }
}