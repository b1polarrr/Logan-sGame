package com.mercury.poker.engine;

import com.mercury.poker.engine.model.Table;

/**
 * 多游戏引擎统一接口。牌桌实时状态仍在内存，由具体实现持有。
 */
public interface GameEngine {

    Table getTable();

    int getSmallBlind();

    int getBigBlind();

    void startNewHand();

    /** 摊牌展示结束后由调度器调用，开始下一局；成功返回 true */
    boolean startNextHand();

    /** 取走并清空本局摊牌结果（若有） */
    HandShowdownResult consumePendingShowdown();

    void playerFold(int seatIndex);

    void playerBet(int seatIndex, int targetTotalBet);

    /** 跟注：筹码不足时自动全下 */
    void playerCall(int seatIndex);

    /** 筹码归零后补充买入 */
    void playerRebuy(int seatIndex, int amount);

    /** 是否可开新局（至少两人有筹码且当前无进行中的手牌） */
    boolean canStartNewHand();

    /** 全员无法行动且公牌未发完，需继续跑牌 */
    boolean needsRunout();

    /**
     * 跑牌：发一轮公牌（翻牌 3 张或转/河各 1 张）；发满后摊牌。
     *
     * @return true 表示后续还有未发的街
     */
    boolean advanceRunoutStreet();
}