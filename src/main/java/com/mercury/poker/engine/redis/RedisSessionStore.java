package com.mercury.poker.engine.redis;

import io.lettuce.core.api.sync.RedisCommands;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Redis 会话持久化：session:{token} → 玩家与房间/座位信息。
 */
public class RedisSessionStore {
    private static final long SESSION_TTL_SECONDS = 24 * 60 * 60;
    private static final RedisSessionStore INSTANCE = new RedisSessionStore();

    private RedisSessionStore() {
    }

    public static RedisSessionStore getINSTANCE() {
        return INSTANCE;
    }

    public String createSession(String userId, String username, boolean authenticated) {
        String sessionToken = UUID.randomUUID().toString().replace("-", "");
        save(sessionToken, userId, username, null, -1, authenticated);
        return sessionToken;
    }

    public void save(
            String sessionToken,
            String userId,
            String username,
            String roomId,
            int seatIndex,
            boolean authenticated
    ) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return;
        }
        RedisCommands<String, String> redis = RedisClientFactory.command();
        Map<String, String> fields = new HashMap<>();
        fields.put("userId", userId);
        fields.put("username", username);
        fields.put("roomId", roomId == null ? "" : roomId);
        fields.put("seatIndex", String.valueOf(seatIndex));
        fields.put("authenticated", authenticated ? "1" : "0");
        fields.put("podId", resolvePodName());
        redis.hset(sessionKey(sessionToken), fields);
        redis.expire(sessionKey(sessionToken), SESSION_TTL_SECONDS);
    }

    public Map<String, String> load(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return Map.of();
        }
        return RedisClientFactory.command().hgetall(sessionKey(sessionToken));
    }

    public void delete(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return;
        }
        RedisClientFactory.command().del(sessionKey(sessionToken));
    }

    private String sessionKey(String sessionToken) {
        return "session:" + sessionToken;
    }

    private String resolvePodName() {
        String podName = System.getenv("POD_NAME");
        if (podName == null || podName.isBlank()) {
            return "local";
        }
        return podName.trim();
    }
}
