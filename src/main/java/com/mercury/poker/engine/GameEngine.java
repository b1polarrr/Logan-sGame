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

    /** 稍后再说（放弃本轮补码询问） */
    void playerDeclineRebuy(int seatIndex);

    /** 起身：占座旁观，不进入后续牌局（局间，或局内已弃牌/未参与） */
    void playerStandUp(int seatIndex);

    /** 起身后再次坐下，恢复参与牌局 */
    void playerSitBack(int seatIndex);

    /** 起身后返回大厅：清空座位，本场盈亏写入已离座列表 */
    void playerLeaveTable(int seatIndex);

    /** 是否可开新局（至少两人有筹码且当前无进行中的手牌） */
    boolean canStartNewHand();

    /** 全员无法行动且公牌未发完，需继续跑牌 */
    boolean needsRunout();

    /**
     * 跑牌：发一轮公牌（翻牌 3 张或转/河各 1 张）；发满后按规则摊牌。
     *
     * @return true 表示后续还有未发的街
     */
    boolean advanceRunoutStreet();

    /** 河牌已发完，需延迟后再摊牌（河牌阶段 all-in 除外） */
    boolean needsDelayedRunoutShowdown();

    /** 跑牌结束后的延迟摊牌 */
    void settleRunoutHand();

    /** 修正行动位卡死（已全下/无筹码仍被轮到） */
    void repairStuckTurn();
}