package com.springboot.demo1.configuration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class Redis {

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        //creating the connection
        RedisStandaloneConfiguration redisStandaloneConfiguration =  new RedisStandaloneConfiguration();
        //localHost
        redisStandaloneConfiguration.setHostName("localhost");
        //portSet
        // 6379 is the default port the local machine
        redisStandaloneConfiguration.setPort(6379);

        return new JedisConnectionFactory(redisStandaloneConfiguration);
    }

    @Bean
    public RedisTemplate<String ,Object > redisTemplate(){
        //object creation
        RedisTemplate<String, Object> redisTemp = new RedisTemplate<>();
        //Properties for the redis Template
        redisTemp.setConnectionFactory(jedisConnectionFactory());
        //how my key, value, Hash keys, hash values and serializer will be stored

        redisTemp.setKeySerializer(new StringRedisSerializer());
        redisTemp.setHashKeySerializer(new StringRedisSerializer());
        redisTemp.setValueSerializer(new JdkSerializationRedisSerializer());
        redisTemp.setEnableTransactionSupport(true);
        //After all setting up the properties return object
        redisTemp.afterPropertiesSet();
        return redisTemp;
    }

    @Bean("etagManager")
    public EtagManager etagManager() {
        return new EtagManager();
    }



}
