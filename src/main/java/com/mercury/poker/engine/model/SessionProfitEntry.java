package com.mercury.poker.engine.model;

import java.io.Serializable;

/** 本场盈亏记录（含已离座玩家，供盈亏面板展示） */
public class SessionProfitEntry implements Serializable {
    private final String userId;
    private final String username;
    private final int sessionProfit;
    private final int lastSeatIndex;

    public SessionProfitEntry(String userId, String username, int sessionProfit, int lastSeatIndex) {
        this.userId = userId;
        this.username = username;
        this.sessionProfit = sessionProfit;
        this.lastSeatIndex = lastSeatIndex;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public int getSessionProfit() {
        return sessionProfit;
    }

    public int getLastSeatIndex() {
        return lastSeatIndex;
    }
}
