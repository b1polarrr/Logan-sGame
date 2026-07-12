package com.mercury.poker.network;

import com.mercury.poker.engine.redis.RedisSessionStore;
import io.netty.channel.Channel;
import io.netty.channel.ChannelId;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();

    /** 连接 ID → 玩家会话 */
    private final ConcurrentHashMap<ChannelId, PlayerSession> sessions = new ConcurrentHashMap<>();

    /** 房间 ID → 该房间内所有连接 */
    private final ConcurrentHashMap<String, Set<Channel>> roomChannels = new ConcurrentHashMap<>();

    private SessionManager() {
    }

    public static SessionManager getINSTANCE() {
        return INSTANCE;
    }

    /** 连接建立：创建临时访客身份，并存入 sessions */
    public PlayerSession onConnect(Channel channel) {
        String shortId = channel.id().asShortText();
        String userId = shortId;
        String username = "玩家_" + shortId.substring(0, Math.min(4, shortId.length()));

        PlayerSession session = new PlayerSession(userId, username, false);
        String sessionToken = RedisSessionStore.getINSTANCE().createSession(userId, username, false);
        session.setSessionToken(sessionToken);
        sessions.put(channel.id(), session);
        return session;
    }

    /** 断开连接：标记离线、持久化并从内存移除 */
    public void onDisconnect(Channel channel) {
        PlayerSession session = sessions.get(channel.id());
        if (session == null) {
            sessions.remove(channel.id());
            return;
        }
        String roomId = session.getRoomId();
        String userId = session.getUserId();
        int seatIndex = session.getSeatIndex();
        persistSessionData(session);
        if (roomId != null) {
            Set<Channel> channels = roomChannels.get(roomId);
            if (channels != null) {
                channels.remove(channel);
            }
        }
        // 先移出本连接，再判断是否还有同账号新连接占座，避免重连竞态把 isOnline 打回 false
        sessions.remove(channel.id());
        if (roomId != null && seatIndex >= 0
                && !hasActiveSeatedSession(userId, roomId)) {
            RoomRouter.getInstance().markPlayerOffline(userId, roomId);
        }
    }

    /**
     * 该 userId 在指定房间是否仍有其它已坐下的活跃连接（用于断线时避免误标离线）。
     */
    public boolean hasActiveSeatedSession(String userId, String roomId) {
        if (userId == null || roomId == null) {
            return false;
        }
        for (PlayerSession otherSession : sessions.values()) {
            if (otherSession == null) {
                continue;
            }
            if (userId.equals(otherSession.getUserId())
                    && roomId.equals(otherSession.getRoomId())
                    && otherSession.getSeatIndex() >= 0) {
                return true;
            }
        }
        return false;
    }

    /** 加入房间：记录 roomId，并把 channel 加入该房间集合 */
    public void joinRoom(Channel channel, String roomId) {
        PlayerSession session = sessions.get(channel.id());
        if (session == null) {
            throw new IllegalStateException("会话不存在");
        }
        session.setRoomId(roomId);
        roomChannels.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(channel);
        persistSession(channel);
    }

    /** 坐下：记录座位号 */
    public void sitDown(Channel channel, int seatIndex) {
        PlayerSession session = sessions.get(channel.id());
        if (session == null) {
            throw new IllegalStateException("会话不存在");
        }
        session.setSeatIndex(seatIndex);
        persistSession(channel);
    }

    /** 根据连接获取会话 */
    public PlayerSession getSession(Channel channel) {
        return sessions.get(channel.id());
    }

    /** 获取某房间内所有在线连接（广播快照时用） */
    public Collection<Channel> getRoomChannels(String roomId) {
        Set<Channel> channels = roomChannels.get(roomId);
        if (channels == null) {
            return Collections.emptyList();
        }
        return channels;
    }

    /**
     * 断线重连：用 Redis 中的会话数据替换当前连接上的临时会话，并重新加入房间频道。
     */
    public void restoreSession(
            Channel channel,
            String sessionToken,
            String userId,
            String username,
            String roomId,
            int seatIndex,
            boolean authenticated
    ) {
        PlayerSession session = new PlayerSession(userId, username, authenticated);
        session.setSessionToken(sessionToken);
        session.setRoomId(roomId);
        session.setSeatIndex(seatIndex);
        sessions.put(channel.id(), session);
        if (roomId != null && !roomId.isBlank()) {
            roomChannels.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(channel);
        }
        persistSession(channel);
    }

    /** 登录成功：绑定账号并写回 Redis */
    public void bindLogin(Channel channel, String userId, String username) {
        PlayerSession session = sessions.get(channel.id());
        if (session == null) {
            throw new IllegalStateException("会话不存在");
        }
        session.bindIdentity(userId, username);
        persistSession(channel);
    }

    /** 房间已销毁时清除本地房间/座位信息并写回 Redis */
    public void clearRoom(Channel channel) {
        PlayerSession session = sessions.get(channel.id());
        if (session == null) {
            return;
        }
        String previousRoomId = session.getRoomId();
        if (previousRoomId != null) {
            Set<Channel> channels = roomChannels.get(previousRoomId);
            if (channels != null) {
                channels.remove(channel);
            }
        }
        session.setRoomId(null);
        session.setSeatIndex(-1);
        persistSession(channel);
    }

    private void persistSession(Channel channel) {
        PlayerSession session = sessions.get(channel.id());
        if (session == null) {
            return;
        }
        persistSessionData(session);
    }

    private void persistSessionData(PlayerSession session) {
        if (session.getSessionToken() == null) {
            return;
        }
        RedisSessionStore.getINSTANCE().save(
                session.getSessionToken(),
                session.getUserId(),
                session.getUsername(),
                session.getRoomId(),
                session.getSeatIndex(),
                session.isAuthenticated()
        );
    }
}
