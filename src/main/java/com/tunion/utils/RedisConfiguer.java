package com.tunion.utils;

import com.tunion.cores.framework.spring.SpringContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 集成RedisTemplate
 */
@Configuration
@EnableAutoConfiguration
public class RedisConfiguer {

    private static Logger logger = LoggerFactory.getLogger(RedisConfiguer.class);

    @Bean
    public SpringContextHolder springContextHolder()
    {
        SpringContextHolder springContextHolder = new SpringContextHolder();

        return springContextHolder;
    }

    /**
     * @return
     * @Bean 和 @ConfigurationProperties
     * 该功能在官方文档是没有提到的，我们可以把@ConfigurationProperties和@Bean和在一起使用。
     * 举个例子，我们需要用@Bean配置一个Config对象，Config对象有a，b，c成员变量需要配置，
     * 那么我们只要在yml或properties中定义了a=1,b=2,c=3，
     * 然后通过@ConfigurationProperties就能把值注入进Config对象中
     */
    @Bean
    @ConfigurationProperties(prefix = "spring.redis.pool")
    public JedisPoolConfig getRedisConfig() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        return jedisPoolConfig;
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.redis")
    public JedisConnectionFactory getConnectionFactory() {
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        JedisPoolConfig jedisPoolConfig = getRedisConfig();
        jedisConnectionFactory.setPoolConfig(jedisPoolConfig);
        return jedisConnectionFactory;
    }

    @Bean
    public RedisTemplate<?, ?> getRedisTemplate() {
        JedisConnectionFactory jedisConnectionFactory = getConnectionFactory();
        logger.info("hostName:{},passwd:{}",jedisConnectionFactory.getHostName(),jedisConnectionFactory.getPassword());
        logger.info("JedisConnectionFactory bean init success.");
        RedisTemplate<?, ?> redisTemplate = new StringRedisTemplate(getConnectionFactory());
        return redisTemplate;
    }
}
