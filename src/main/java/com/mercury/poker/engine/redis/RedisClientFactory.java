package com.mercury.poker.engine.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

public class RedisClientFactory {
    private static final String DEFAULT_URL = "redis://localhost:6379";
    private static RedisClient redisClient;
    private static StatefulRedisConnection<String ,String> connection;
    private RedisClientFactory(){

    }
    public static synchronized RedisCommands<String, String> command(){
        if (connection == null || !connection.isOpen()){
            String redisUrl = System.getenv("REDIS_URL");
            if (redisUrl == null || redisUrl.isBlank()){
                redisUrl = DEFAULT_URL;
            }
            redisClient = RedisClient.create(redisUrl);
            connection = redisClient.connect();
        }
        return connection.sync();
    }
}
