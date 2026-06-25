package com.mercury.poker.engine.model;

import java.util.List;

public record HandValue (
    HandCategory category,   //牌型
    List<Integer> ranks,    //用于平局判定的点数权重列表（按重要性排序）
    List<Card> bestFive     //7选5
)implements Comparable<HandValue> {
    @Override
    public int compareTo(HandValue other){
        //1.比牌型
        if (this.category != other.category){
            return Integer.compare(this.category.getRank(), other.category.getRank());
        }
        //2.牌型相同，逐个比较
        for (int i = 0;i<this.ranks.size();i++){
            if (!this.ranks.get(i).equals(other.ranks.get(i))){
                return Integer.compare(this.ranks.get(i),other.ranks.get(i));
            }
        }
        return 0;
    }
}
