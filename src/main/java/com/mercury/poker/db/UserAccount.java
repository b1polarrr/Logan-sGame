package com.mercury.poker.db;

public class UserAccount {
    private final String userId;
    private final String username;
    private final String passwordHash;

    public UserAccount(String userId, String username, String passwordHash) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
