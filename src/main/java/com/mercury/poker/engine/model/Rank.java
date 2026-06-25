package com.mercury.poker.engine.model;

public enum Rank {
    TWO("2", 2), THREE("3", 3), FOUR("4", 4), FIVE("5", 5),
    SIX("6", 6), SEVEN("7", 7), EIGHT("8", 8), NINE("9", 9),
    TEN("10", 10), JACK("J", 11), QUEEN("Q", 12), KING("K", 13), ACE("A", 14);
    private final String display;
    private final int power;//高牌

    Rank(String display,int power){
        this.display = display;
        this.power = power;
    }
    public String getDisplay(){return display;}

    public int getPower() {
        return power;
    }
}
