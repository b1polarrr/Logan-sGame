package com.mercury.poker.engine.model;

public record Card(Rank rank,Suit suit) {
    /**
     低13位表示点数（2-A）
     随后4位表示花色
     */
    public int getBigMask(){
        return (1<<rank.ordinal()) | (1<<(suit.ordinal()+16));
    }

    /**
     * 获取排序权重值
     */
    public int getPower(){
        return rank.getPower();
    }
    @Override
    public String toString(){
        return suit.getSymbol() + rank.getDisplay();
    }
}
