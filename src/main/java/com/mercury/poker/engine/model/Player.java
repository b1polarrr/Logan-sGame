package com.mercury.poker.engine.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Player implements Serializable {
    private final String userId;   //玩家ID
    private final String username; //玩家名称
    private int seatIndex; //座位号
    private int chips;
    /** 本场累计买入（含初始坐下与补码） */
    private int sessionBuyIn;
    private int currentBet; //当前轮次下注额
    /** 本局累计投入（含已 sweep 进底池的部分，用于边池结算） */
    private int handContribution;
    private final List<Card> holeCards = new ArrayList<>(); //底牌（2张）
    private boolean isFolded; //是否已弃牌
    private boolean isAllIn; //是否已ALL in
    private boolean isActive; //是否在当前局中
    private boolean isOnline; //网络状态
    private boolean isReady; //是否已准备（局间等待开局）
    /** 是否补码：初始 true；询问后选稍后再说为 false；补码后为 true */
    private boolean willRebuy;
    /** 局间补码后跳过紧接的一局（筹码已到账但本局不参与） */
    private boolean rebuyPendingSitOut;

    public Player (String userId,String username,int chips){
        this.userId = userId;
        this.username = username;
        this.chips = chips;
        this.sessionBuyIn = chips;
        this.isFolded = false;
        this.isAllIn = false;
        this.isOnline = true;
        this.isReady = false;
        this.willRebuy = true;
        this.rebuyPendingSitOut = false;
        this.currentBet = 0;
        this.handContribution = 0;
        this.isActive = chips > 0;
    }

    //重置每局的手牌和下注状态
    public void resetForNewHand(){
        this.holeCards.clear();
        this.currentBet = 0;
        this.handContribution = 0;
        this.isFolded = false;
        this.isAllIn = false;
        this.isActive = this.chips > 0 && !this.rebuyPendingSitOut;
        if (this.rebuyPendingSitOut) {
            this.rebuyPendingSitOut = false;
        }
    }

    /** 局间补码：筹码到账，清除全下标记，本局不参与 */
    public void applyBetweenHandsRebuy() {
        this.isAllIn = false;
        this.rebuyPendingSitOut = true;
        this.isActive = false;
    }

    //下注
    public void bet(int amount){
        if (amount>this.chips){
            throw new IllegalArgumentException("筹码不足");
        }
        this.chips -= amount;
        this.currentBet += amount;
        this.handContribution += amount;
        if (this.chips == 0){
            this.isAllIn = true;
        }
    }

    //筹码不足All in
    public int betUpTo(int amount){
        int actual = Math.min(amount,this.chips);
        if (actual <= 0){
            return 0;
        }
        bet(actual);
        return actual;
    }

    public void addCard(Card card){
        if (holeCards.size() >= 2){
            throw new IllegalStateException("底牌不难超过2张");
        }
        holeCards.add(card);
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public int getSeatIndex() {
        return seatIndex;
    }

    public void setSeatIndex(int seatIndex) {
        this.seatIndex = seatIndex;
    }

    public int getChips() {
        return chips;
    }
    public void addChips(int amount){
        this.chips += amount;
    }

    public int getSessionBuyIn() {
        return sessionBuyIn;
    }

    public void addSessionBuyIn(int amount) {
        this.sessionBuyIn += amount;
    }

    /** 本场盈亏：当前筹码减去累计买入 */
    public int getSessionProfit() {
        return chips - sessionBuyIn;
    }

    public int getHandContribution() {
        return handContribution;
    }

    public int getCurrentBet() {
        return currentBet;
    }
    public void clearCurrentBet(){this.currentBet = 0;}

    public List<Card> getHoleCards() {
        return holeCards;
    }

    public boolean isFolded() {
        return isFolded;
    }

    public void setFolded(boolean folded) {
        isFolded = folded;
    }

    public boolean isAllIn() {
        return isAllIn;
    }

    /** 局内筹码归零时补齐全下标记，避免 isAllIn 与 chips 不一致卡住行动轮 */
    public void syncAllInState() {
        if (this.isActive && !this.isFolded && this.chips == 0) {
            this.isAllIn = true;
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    public boolean isWillRebuy() {
        return willRebuy;
    }

    public void setWillRebuy(boolean willRebuy) {
        this.willRebuy = willRebuy;
    }

    public boolean isRebuyPendingSitOut() {
        return rebuyPendingSitOut;
    }
}
