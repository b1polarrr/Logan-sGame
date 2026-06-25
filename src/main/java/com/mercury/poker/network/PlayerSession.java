package com.mercury.poker.network;

public class PlayerSession {
    private final String userId;
    private final String username;
    private String roomId; //加入房间后赋值
    private int seatIndex = -1; //坐下后赋值
    private String sessionToken;

    public PlayerSession(String userId, String username){
        this.userId = userId;
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public int getSeatIndex() {
        return seatIndex;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setSeatIndex(int seatIndex) {
        this.seatIndex = seatIndex;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
}
