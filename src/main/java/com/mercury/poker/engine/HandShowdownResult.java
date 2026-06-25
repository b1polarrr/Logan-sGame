package com.mercury.poker.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 一局结束时的摊牌/收池结果（在下一局开始前展示） */
public class HandShowdownResult {

    private final String roomId;
    private final int potTotal;
    private final String reason;
    private final List<PlayerLine> players;

    public HandShowdownResult(String roomId, int potTotal, String reason, List<PlayerLine> players) {
        this.roomId = roomId;
        this.potTotal = potTotal;
        this.reason = reason;
        this.players = new ArrayList<>(players);
    }

    public String getRoomId() {
        return roomId;
    }

    public int getPotTotal() {
        return potTotal;
    }

    public String getReason() {
        return reason;
    }

    public List<PlayerLine> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public static class PlayerLine {
        private final int seatIndex;
        private final String username;
        private final List<String> holeCards;
        private final int chipsWon;
        private final boolean winner;
        private final String handTypeName;

        public PlayerLine(
                int seatIndex,
                String username,
                List<String> holeCards,
                int chipsWon,
                boolean winner,
                String handTypeName
        ) {
            this.seatIndex = seatIndex;
            this.username = username;
            this.holeCards = new ArrayList<>(holeCards);
            this.chipsWon = chipsWon;
            this.winner = winner;
            this.handTypeName = handTypeName == null ? "" : handTypeName;
        }

        public String getHandTypeName() {
            return handTypeName;
        }

        public int getSeatIndex() {
            return seatIndex;
        }

        public String getUsername() {
            return username;
        }

        public List<String> getHoleCards() {
            return Collections.unmodifiableList(holeCards);
        }

        public int getChipsWon() {
            return chipsWon;
        }

        public boolean isWinner() {
            return winner;
        }
    }
}
