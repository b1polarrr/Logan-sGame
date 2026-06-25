package com.mercury.poker.engine.model;

public enum HandCategory {
    HIGH_CARD(1, "高牌"),
    ONE_PAIR(2, "对子"),
    TWO_PAIR(3, "两对"),
    THREE_OF_A_KIND(4, "三条"),
    STRAIGHT(5, "顺子"),
    FLUSH(6, "同花"),
    FULL_HOUSE(7, "葫芦"),
    FOUR_OF_A_KIND(8, "四条"),
    STRAIGHT_FLUSH(9, "同花顺"),
    ROYAL_FLUSH(10, "皇家同花顺");
    private final int rank;
    private final String description;

    HandCategory(int rank,String description){
        this.rank = rank;
        this.description =description;
    }

    public int getRank() {
        return rank;
    }

    public String getDescription() {
        return description;
    }
}
