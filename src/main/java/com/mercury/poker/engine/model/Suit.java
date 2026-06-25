package com.mercury.poker.engine.model;

public enum Suit {
    SPADES("♠"),   // 黑
    HEARTS("♥"),   // 红
    CLUBS("♣"),    // 梅
    DIAMONDS("♦"); // 方

    private final String symbol;
    Suit(String symbol){
        this.symbol =  symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
