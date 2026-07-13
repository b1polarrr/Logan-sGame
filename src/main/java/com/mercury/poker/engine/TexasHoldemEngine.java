package com.mercury.poker.engine;

import com.mercury.poker.engine.model.Table;

/**
 * 德州扑克引擎：委托现有 GameManager，不重写规则逻辑。
 */
public class TexasHoldemEngine implements GameEngine {

    private final GameManager gameManager;

    public TexasHoldemEngine(Table table, int smallBlind, int bigBlind) {
        this.gameManager = new GameManager(table, smallBlind, bigBlind);
    }

    @Override
    public Table getTable() {
        return gameManager.getTable();
    }

    @Override
    public int getSmallBlind() {
        return gameManager.getSmallBlind();
    }

    @Override
    public int getBigBlind() {
        return gameManager.getBigBlind();
    }

    @Override
    public void startNewHand() {
        gameManager.startNewHand();
    }

    @Override
    public boolean startNextHand() {
        return gameManager.startNextHand();
    }

    @Override
    public HandShowdownResult consumePendingShowdown() {
        return gameManager.consumePendingShowdown();
    }

    @Override
    public void playerFold(int seatIndex) {
        gameManager.playerFold(seatIndex);
    }

    @Override
    public void playerBet(int seatIndex, int targetTotalBet) {
        gameManager.playerBet(seatIndex, targetTotalBet);
    }

    @Override
    public void playerCall(int seatIndex) {
        gameManager.playerCall(seatIndex);
    }

    @Override
    public void playerRebuy(int seatIndex, int amount) {
        gameManager.playerRebuy(seatIndex, amount);
    }

    @Override
    public void playerDeclineRebuy(int seatIndex) {
        gameManager.playerDeclineRebuy(seatIndex);
    }

    @Override
    public void playerStandUp(int seatIndex) {
        gameManager.playerStandUp(seatIndex);
    }

    @Override
    public void playerSitBack(int seatIndex) {
        gameManager.playerSitBack(seatIndex);
    }

    @Override
    public void playerLeaveTable(int seatIndex) {
        gameManager.playerLeaveTable(seatIndex);
    }

    @Override
    public boolean canStartNewHand() {
        return gameManager.canStartNewHand();
    }

    @Override
    public boolean needsRunout() {
        return gameManager.needsRunout();
    }

    @Override
    public boolean advanceRunoutStreet() {
        return gameManager.advanceRunoutStreet();
    }

    @Override
    public boolean needsDelayedRunoutShowdown() {
        return gameManager.needsDelayedRunoutShowdown();
    }

    @Override
    public void settleRunoutHand() {
        gameManager.settleRunoutHand();
    }

    @Override
    public void repairStuckTurn() {
        gameManager.repairStuckTurn();
    }
}