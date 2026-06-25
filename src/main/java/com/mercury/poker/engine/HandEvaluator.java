package com.mercury.poker.engine;

import com.mercury.poker.engine.model.Card;
import com.mercury.poker.engine.model.HandCategory;
import com.mercury.poker.engine.model.HandValue;
import com.mercury.poker.engine.model.Rank;

import java.util.*;
import java.util.stream.Collectors;

public class HandEvaluator {
    /**
     * 7选5
     * @param sevenCards
     * @return
     */
    public HandValue evaluate(List<Card> sevenCards){
        //生成所有5张牌的组合
        List<List<Card>> combinations = generateCombinations(sevenCards,5);
        //给每种组合判断，返回最大的牌
        return combinations.stream()
                .map(this::evaluateFiveCards)
                .max(HandValue::compareTo)
                .orElseThrow();
    }
    /**
     *5张牌的等级与权重
     */
    private HandValue evaluateFiveCards(List<Card> fiveCards){
        //A.预处理：按点数降序排序
        List<Card> sorted = fiveCards.stream()
                .sorted(Comparator.comparing(Card::getPower).reversed())
                .collect(Collectors.toList());
        //B.统计点数频率
        Map<Rank,Long> rankCounts = sorted.stream()
                .collect(Collectors.groupingBy(Card::rank,Collectors.counting()));
        //将频率值提取并排序，例如[3,2]为葫芦
        List<Long> counts = rankCounts.values().stream()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        //C.基础判定：同花和顺子
        boolean isFlush = sorted.stream().map(Card::suit).distinct().count()==1;
        boolean isStraight = checkStraight(sorted);
        //D.平局判断踢脚
        List<Integer> ranks = rankCounts.entrySet().stream()
                .sorted((e1,e2)->{
                    int res = e2.getValue().compareTo(e1.getValue());
                    if (res == 0) return e2.getKey().getPower() - e1.getKey().getPower();
                    return res;
                })
                .map(e->e.getKey().getPower())
                .collect(Collectors.toList());
        //E.牌型定性

        //1.同花顺
        if (isFlush && isStraight){
            //特例12345（5为最大权重）
            if (sorted.get(0).rank() == Rank.ACE && sorted.get(1).rank() == Rank.FIVE){
                return new HandValue(HandCategory.STRAIGHT_FLUSH,List.of(5),sorted);
            }
            return new HandValue(HandCategory.STRAIGHT_FLUSH,List.of(sorted.get(0).getPower()),sorted);
        }
        //2.四条
        if (counts.get(0) == 4){
            return new HandValue(HandCategory.FOUR_OF_A_KIND,ranks,sorted);
        }
        //3.葫芦
        if (counts.get(0)==3 && counts.get(1) == 2){
            return new HandValue(HandCategory.FULL_HOUSE,ranks,sorted);
        }
        //4.同花
        if (isFlush){
            return new HandValue(HandCategory.FLUSH,ranks,sorted);
        }
        //5.顺子
        if (isStraight){
            if (sorted.get(0).rank() == Rank.ACE && sorted.get(1).rank() == Rank.FIVE){
                return new HandValue(HandCategory.STRAIGHT,List.of(5),sorted);
            }
            return new HandValue(HandCategory.STRAIGHT,List.of(sorted.get(0).getPower()),sorted);
        }
        //6.三条
        if (counts.get(0) == 3){
            return new HandValue(HandCategory.THREE_OF_A_KIND,ranks,sorted);
        }
        //7.俩对
        if (counts.get(0) == 2 && counts.get(1) == 2){
            return new HandValue(HandCategory.TWO_PAIR,ranks,sorted);
        }
        //8.一对
        if (counts.get(0) == 2){
            return new HandValue(HandCategory.ONE_PAIR,ranks,sorted);
        }
        //9.高牌
        return new HandValue(HandCategory.HIGH_CARD,ranks,sorted);
    }

    /**
     * 判断顺子
     * @param sorted
     * @return
     */
    private boolean checkStraight(List<Card> sorted){
        //特例 12345
        boolean hasAceToFive = sorted.get(0).rank() == Rank.ACE &&
                sorted.get(1).rank() == Rank.FIVE &&
                sorted.get(2).rank() == Rank.FOUR &&
                sorted.get(3).rank() == Rank.THREE &&
                sorted.get(4).rank() == Rank.TWO;
        if (hasAceToFive) return  true;
        //相邻牌Power差值为1
        for (int i = 0;i<sorted.size()-1;i++){
            if (sorted.get(i).rank().getPower() - sorted.get(i+1).rank().getPower() != 1){
                return false;
            }
        }
        return true;
    }


    /**
     * 生成组合的递归算法
     * @param list
     * @param n
     * @return
     */
    private List<List<Card>> generateCombinations(List<Card> list,int n){
        List<List<Card>> result = new ArrayList<>();
        combinationsInternal(list,n,0,new ArrayList<>(),result);
        return result;
    }
    private void combinationsInternal(List<Card> list,int n,int start,List<Card> current,List<List<Card>> result){
        if (current.size() == n){
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start;i<list.size();i++){
            current.add(list.get(i));
            combinationsInternal(list,n,i+1,current,result);
            current.remove(current.size()-1); //回溯
        }
    }
}
