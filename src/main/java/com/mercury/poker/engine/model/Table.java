package com.mercury.poker.engine.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Table implements Serializable {
    private final String roomId; //房间ID
    private final Player[] seats; //固定座位的数组
    private final List<Card> communityCards; //公共牌
    private final Deck deck; //牌堆

    private int pot; //主底池筹码量
    private int currentMaxBet; //当前轮次全场最高的下注额

    private int dealerIndex; //庄家（Button）的座位号
    private int smallBlindIndex; //小盲座位号
    private int bigBlindIndex; //大盲座位号
    private int currentTurnIndex; //正在操作的玩家座位号

    public Table(String roomId,int maxSeats){
        this.roomId = roomId;
        this.seats = new Player[maxSeats];
        this.communityCards = new ArrayList<>();
        this.deck = new Deck();
        this.pot = 0;
        this.currentMaxBet = 0;
        this.dealerIndex = 0;
        this.currentTurnIndex = -1;
    }

    //玩家坐下
    public void sitDown(Player player, int seatIndex){
        if (seatIndex < 0 || seatIndex >=seats.length){
            throw new IllegalArgumentException("非法座位号");
        }
        if (seats[seatIndex] != null){
            throw new IllegalStateException("该座位已有人");
        }
        seats[seatIndex] = player;
        player.setSeatIndex(seatIndex);
    }

    // 玩家站起/离开房间
    public void standUp(int seatIndex){
        if (seatIndex >= 0 && seatIndex < seats.length){
            seats[seatIndex] = null;
        }
    }

    //重置牌桌，准备新一局
    public void resetForNewHand(){
        this.communityCards.clear();
        this.deck.reset();
        this.deck.shuffle();
        this.pot = 0;
        this.currentMaxBet = 0;

        for (Player player : seats){
            if (player != null){
                player.resetForNewHand();
            }
        }
    }

    public void addCommunityCard(Card card){
        if (communityCards.size() >= 5){
            throw new IllegalStateException("公牌不难超过5张");
        }
        communityCards.add(card);
    }

    public int getBigBlindIndex() {
        return bigBlindIndex;
    }

    public int getSmallBlindIndex() {
        return smallBlindIndex;
    }

    //分完筹码后清空pot
    public void clearPot(){
        this.pot = 0;
    }

    public void setSmallBlindIndex(int smallBlindIndex) {
        this.smallBlindIndex = smallBlindIndex;
    }

    public void setBigBlindIndex(int bigBlindIndex) {
        this.bigBlindIndex = bigBlindIndex;
    }

    public String getRoomId() {
        return roomId;
    }

    public Player[] getSeats() {
        return seats;
    }

    public List<Card> getCommunityCards() {
        return communityCards;
    }

    public Deck getDeck() {
        return deck;
    }

    public int getPot() {
        return pot;
    }
    public void addPot(int amount){this.pot += amount;}

    public int getCurrentMaxBet() {
        return currentMaxBet;
    }

    public void setCurrentMaxBet(int currentMaxBet) {
        this.currentMaxBet = currentMaxBet;
    }

    public int getDealerIndex() {
        return dealerIndex;
    }

    public void setDealerIndex(int dealerIndex) {
        this.dealerIndex = dealerIndex;
    }

    public int getCurrentTurnIndex() {
        return currentTurnIndex;
    }

    public void setCurrentTurnIndex(int currentTurnIndex) {
        this.currentTurnIndex = currentTurnIndex;
    }
}
