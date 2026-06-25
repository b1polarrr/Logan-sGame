package com.mercury.poker.engine.redis;

/**
 * 大厅房间元数据（纯 Java，不依赖 Protobuf 生成类）。
 */
public class LobbyRoomMeta {

    private final String roomId;
    private final int gameType;
    private final int maxSeats;
    private final int seatedCount;
    private final int smallBlind;
    private final int bigBlind;

    public LobbyRoomMeta(
            String roomId,
            int gameType,
            int maxSeats,
            int seatedCount,
            int smallBlind,
            int bigBlind) {
        this.roomId = roomId;
        this.gameType = gameType;
        this.maxSeats = maxSeats;
        this.seatedCount = seatedCount;
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
    }

    public String getRoomId() {
        return roomId;
    }

    public int getGameType() {
        return gameType;
    }

    public int getMaxSeats() {
        return maxSeats;
    }

    public int getSeatedCount() {
        return seatedCount;
    }

    public int getSmallBlind() {
        return smallBlind;
    }

    public int getBigBlind() {
        return bigBlind;
    }
}
