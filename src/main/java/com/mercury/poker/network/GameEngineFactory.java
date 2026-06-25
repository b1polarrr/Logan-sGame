package com.mercury.poker.network;

import com.mercury.poker.engine.GameEngine;
import com.mercury.poker.engine.TexasHoldemEngine;
import com.mercury.poker.engine.model.Table;
import com.mercury.poker.network.protocol.GameType;

public final class GameEngineFactory {

    private GameEngineFactory() {
    }

    public static GameEngine create(
            GameType gameType,
            Table table,
            int smallBlind,
            int bigBlind) {
        if (gameType == GameType.TEXAS_HOLDEM) {
            return new TexasHoldemEngine(table, smallBlind, bigBlind);
        }
        throw new IllegalArgumentException("暂不支持的游戏类型: " + gameType);
    }
}