package com.mercury.poker.network;

public class PlayerSession {
    private String userId;
    private String username;
    private String roomId; //加入房间后赋值
    private int seatIndex = -1; //坐下后赋值
    private String sessionToken;
    private boolean authenticated;

    public PlayerSession(String userId, String username) {
        this(userId, username, false);
    }

    public PlayerSession(String userId, String username, boolean authenticated) {
        this.userId = userId;
        this.username = username;
        this.authenticated = authenticated;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    /** 登录成功后绑定真实账号身份 */
    public void bindIdentity(String userId, String username) {
        this.userId = userId;
        this.username = username;
        this.authenticated = true;
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
