package com.mercury.poker.engine.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Stack;


/**
 * 牌堆类：初始化、洗牌、发牌
 */
public class Deck {
    private final Stack<Card> cards = new Stack<>();
    public Deck(){
        reset();
    }

    /**
     * 重置并初始化52张牌
     */
    public void reset(){
        cards.clear();
        for (Suit suit : Suit.values()){
            for (Rank rank : Rank.values()){
                cards.push(new Card(rank,suit));
            }
        }
    }

    /**
     * 洗牌：
     */
    public void shuffle(){
        Collections.shuffle(cards);
    }
    /**
     * 发牌
     * @return
     */
    public Card deal(){
        if (cards.isEmpty()){
            throw new IllegalStateException("牌堆已空，无法发牌");
        }
        return  cards.pop();
    }

    /**
     * 获取剩余牌数
     */
    public int remaining(){
        return cards.size();
    }
}
