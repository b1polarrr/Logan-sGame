package com.mercury.poker.engine.redis;

import io.lettuce.core.ScoredValue;
import io.lettuce.core.api.sync.RedisCommands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Redis 只存房间元数据与大厅列表，不存牌桌实时状态。
 */
public class RedisRoomRegistry {

    private static final String LOBBY_ROOMS_KEY = "lobby:rooms";
    private static final int DEFAULT_GAME_TYPE = 1;
    private static final RedisRoomRegistry INSTANCE = new RedisRoomRegistry();

    private RedisRoomRegistry() {
    }

    public static RedisRoomRegistry getINSTANCE() {
        return INSTANCE;
    }

    public void registerRoom(LobbyRoomMeta lobbyRoomMeta) {
        RedisCommands<String, String> redis = RedisClientFactory.command();
        String roomId = lobbyRoomMeta.getRoomId();
        String podId = resolvePodName();

        redis.hset(roomKey(roomId), Map.of(
                "gameType", String.valueOf(lobbyRoomMeta.getGameType()),
                "maxSeats", String.valueOf(lobbyRoomMeta.getMaxSeats()),
                "seatedCount", String.valueOf(lobbyRoomMeta.getSeatedCount()),
                "smallBlind", String.valueOf(lobbyRoomMeta.getSmallBlind()),
                "bigBlind", String.valueOf(lobbyRoomMeta.getBigBlind()),
                "podId", podId
        ));
        redis.zadd(LOBBY_ROOMS_KEY, System.currentTimeMillis(), roomId);
        redis.sadd(podRoomsKey(podId), roomId);
    }

    public void updateSeatedCount(String roomId, int seatedCount) {
        RedisCommands<String, String> redis = RedisClientFactory.command();
        if (redis.exists(roomKey(roomId)) == 0) {
            return;
        }
        redis.hset(roomKey(roomId), "seatedCount", String.valueOf(seatedCount));
    }

    public List<LobbyRoomMeta> listAllRooms() {
        RedisCommands<String, String> redis = RedisClientFactory.command();
        List<ScoredValue<String>> scoredValues = redis.zrangeWithScores(LOBBY_ROOMS_KEY, 0, -1);
        List<LobbyRoomMeta> lobbyRoomMetaList = new ArrayList<>();
        for (ScoredValue<String> scoredValue : scoredValues) {
            String roomId = scoredValue.getValue();
            Map<String, String> meta = redis.hgetall(roomKey(roomId));
            if (meta.isEmpty()) {
                continue;
            }
            lobbyRoomMetaList.add(new LobbyRoomMeta(
                    roomId,
                    parseInt(meta.get("gameType"), DEFAULT_GAME_TYPE),
                    parseInt(meta.get("maxSeats"), 6),
                    parseInt(meta.get("seatedCount"), 0),
                    parseInt(meta.get("smallBlind"), 10),
                    parseInt(meta.get("bigBlind"), 20)
            ));
        }
        return lobbyRoomMetaList;
    }

    public boolean roomExists(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return false;
        }
        return RedisClientFactory.command().exists(roomKey(roomId)) > 0;
    }

    public String getRoomPodId(String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return null;
        }
        RedisCommands<String, String> redis = RedisClientFactory.command();
        if (redis.exists(roomKey(roomId)) == 0) {
            return null;
        }
        return redis.hget(roomKey(roomId), "podId");
    }

    public void unregisterRoom(String roomId) {
        RedisCommands<String, String> redis = RedisClientFactory.command();
        String podId = redis.hget(roomKey(roomId), "podId");
        redis.del(roomKey(roomId));
        redis.zrem(LOBBY_ROOMS_KEY, roomId);
        if (podId != null && !podId.isBlank()) {
            redis.srem(podRoomsKey(podId), roomId);
        }
    }

    private String resolvePodName() {
        String podName = System.getenv("POD_NAME");
        if (podName == null || podName.isBlank()) {
            return "local";
        }
        return podName.trim();
    }

    private int parseInt(String rawValue, int defaultValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(rawValue);
    }

    private String roomKey(String roomId) {
        return "room:" + roomId + ":meta";
    }

    private String podRoomsKey(String podId) {
        return "pod:" + podId + ":rooms";
    }
}
