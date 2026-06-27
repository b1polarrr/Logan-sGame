package com.mercury.poker.engine;

import com.mercury.poker.engine.model.Card;
import com.mercury.poker.engine.model.HandValue;
import com.mercury.poker.engine.model.Player;
import com.mercury.poker.engine.model.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class GameManager {
    private final Table table;
    private final HandEvaluator evaluator;
    private final int smallBlind;
    private final int bigBlind;
    private int lastAggressorIndex = -1; //行动是否绕一圈回到最后加者
    /** 本下注轮已行动且仍可决策的座位（新街或加注后清空） */
    private final Set<Integer> actedThisRound = new HashSet<>();
    private HandShowdownResult pendingShowdown = null;
    public GameManager(Table table,int smallBlind, int bigBlind){
        this.table = table;
        this.evaluator = new HandEvaluator();
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
    }

    public Table getTable() {
        return table;
    }

    public int getSmallBlind() {
        return smallBlind;
    }

    public int getBigBlind() {
        return bigBlind;
    }

    /**
     * 1.开始新一局（Pre-Flop）
     */
    public void startNewHand(){
        table.resetForNewHand();
        //移动庄家按钮（Button）到下一个有效玩家
        moveToNextDealer();
        //盲注
        postBlinds();
        //发底牌
        dealHoleCards();
        lastAggressorIndex = table.getBigBlindIndex();
        //第一个行动（大盲位的下一个，即UTG）
        int firstPlayerIndex = getNextActiveSeat(table.getBigBlindIndex());
        if (firstPlayerIndex == -1) {
            throw new IllegalStateException("没有可行动的玩家");
        }
        table.setCurrentTurnIndex(firstPlayerIndex);
        actedThisRound.clear();
        pendingShowdown = null;
    }

    public HandShowdownResult consumePendingShowdown() {
        HandShowdownResult result = pendingShowdown;
        pendingShowdown = null;
        return result;
    }

    public boolean startNextHand() {
        if (countSeatedPlayersWithChips() >= 2) {
            startNewHand();
            return true;
        }
        return false;
    }

    /**
     * 补充筹码：仅当当前筹码为 0 时允许。
     */
    public void playerRebuy(int seatIndex, int amount) {
        Player player = table.getSeats()[seatIndex];
        if (player == null) {
            throw new IllegalStateException("座位无人");
        }
        if (player.getChips() > 0) {
            throw new IllegalStateException("仍有筹码，无需补充");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("补充金额须大于 0");
        }
        player.addChips(amount);
        player.addSessionBuyIn(amount);
        player.resetForNewHand();
    }

    public boolean canStartNewHand() {
        // 局间 currentTurnIndex 为 -1；不能用底牌判断（上一局牌要等 startNewHand 才清）
        return table.getCurrentTurnIndex() < 0 && countSeatedPlayersWithChips() >= 2;
    }

    /**
     * 2.Fold
     */
    public void playerFold(int seatIndex){
        validatePlayerTurn(seatIndex);
        Player player = table.getSeats()[seatIndex];
        player.setFolded(true);
        actedThisRound.add(seatIndex);

        checkRoundOrHandOver(seatIndex);
    }

    /**
     * 执行动作：check/call/raise
     */
    public void playerBet(int seatIndex, int targetTotalBet){
        validatePlayerTurn(seatIndex);
        Player player = table.getSeats()[seatIndex];
        if (player == null || player.isFolded() || player.isAllIn()){
            throw new IllegalStateException("无法下注");
        }

        int maxBet = table.getCurrentMaxBet();
        int currentBet = player.getCurrentBet();

        //check
        if (targetTotalBet == currentBet){
            if (maxBet > currentBet){
                throw new IllegalArgumentException("只能call or fold");
            }
            actedThisRound.add(seatIndex);
            checkRoundOrHandOver(seatIndex);
            return;
        }

        //call / raise
        if (targetTotalBet < currentBet){
            throw new IllegalArgumentException("目标下注额不难低于已下注额");
        }
        int maxAllowed = currentBet + player.getChips();
        if (targetTotalBet > maxAllowed) {
            // 筹码不足时自动按全下处理（跟注/加注均可）
            targetTotalBet = maxAllowed;
        }
        if (targetTotalBet <= currentBet) {
            throw new IllegalArgumentException("筹码不足");
        }

        int needed = targetTotalBet - currentBet;
        player.betUpTo(needed);

        if (player.getCurrentBet() > maxBet){
            table.setCurrentMaxBet(player.getCurrentBet());
            lastAggressorIndex = seatIndex;
            actedThisRound.clear();
            actedThisRound.add(seatIndex);
        } else {
            actedThisRound.add(seatIndex);
        }

        checkRoundOrHandOver(seatIndex);
    }

    /**
     * 跟注到当前最高注；筹码不够时投入全部剩余筹码（全下）。
     */
    public void playerCall(int seatIndex) {
        validatePlayerTurn(seatIndex);
        Player player = table.getSeats()[seatIndex];
        if (player == null || player.isFolded() || player.isAllIn()) {
            throw new IllegalStateException("无法跟注");
        }
        int maxBet = table.getCurrentMaxBet();
        int affordableTotal = Math.min(maxBet, player.getCurrentBet() + player.getChips());
        playerBet(seatIndex, affordableTotal);
    }

    /**
     * 4.发公共牌
     */
    public void advanceStreet(int currentStreetCardsCount){
        sweepBetsToPot();
        table.setCurrentMaxBet(0);
        actedThisRound.clear();

        //切一张
        table.getDeck().deal();

        //发牌
        for (int i = 0; i < currentStreetCardsCount; i++){
            table.addCommunityCard(table.getDeck().deal());
        }

        table.setCurrentTurnIndex(getNextSeatCanAct(table.getDealerIndex()));
        lastAggressorIndex = table.getDealerIndex();
    }

    /**
     * 5.showdown
     */
    public void settleHand(){
        sweepBetsToPot();
        int potTotal = table.getPot();

        Player[] seats = table.getSeats();
        List<Player> activePlayer = new ArrayList<>();

        for (Player p : seats){
            if (p != null && !p.isFolded() && p.isActive()){
                activePlayer.add(p);
            }
        }

        if (activePlayer.isEmpty()) {
            table.clearPot();
            actedThisRound.clear();
            table.setCurrentTurnIndex(-1);
            return;
        }

        //只有一个人
        if (activePlayer.size() == 1){
            Player winner = activePlayer.get(0);
            winner.addChips(potTotal);
            pendingShowdown = buildShowdownResult(potTotal, "fold", List.of(winner), potTotal, null);
            table.clearPot();
            actedThisRound.clear();
            table.setCurrentTurnIndex(-1);
            return;
        }

        // 多人比牌（边池）
        Map<Player, HandValue> playerResults = new HashMap<>();
        for (Player player : activePlayer) {
            List<Card> sevenCards = new ArrayList<>(table.getCommunityCards());
            sevenCards.addAll(player.getHoleCards());
            playerResults.put(player, evaluator.evaluate(sevenCards));
        }

        Map<Player, Integer> chipsWonByPlayer = new HashMap<>();
        for (SidePotLayer layer : buildSidePotLayers()) {
            awardSidePotLayer(layer, playerResults, chipsWonByPlayer);
        }

        for (Map.Entry<Player, Integer> entry : chipsWonByPlayer.entrySet()) {
            entry.getKey().addChips(entry.getValue());
        }

        pendingShowdown = buildShowdownResultWithWinnings(
                potTotal, "showdown", playerResults, chipsWonByPlayer);
        table.clearPot();
        actedThisRound.clear();
        table.setCurrentTurnIndex(-1);
    }

    private record SidePotLayer(int amount, List<Player> eligiblePlayers) {}

    private List<SidePotLayer> buildSidePotLayers() {
        Map<Player, Integer> contributions = new HashMap<>();
        for (Player player : table.getSeats()) {
            if (player != null && player.getHandContribution() > 0) {
                contributions.put(player, player.getHandContribution());
            }
        }

        TreeSet<Integer> levels = new TreeSet<>();
        for (int contribution : contributions.values()) {
            levels.add(contribution);
        }

        List<SidePotLayer> layers = new ArrayList<>();
        int previousLevel = 0;
        for (int level : levels) {
            int potSlice = 0;
            List<Player> eligiblePlayers = new ArrayList<>();
            for (Map.Entry<Player, Integer> entry : contributions.entrySet()) {
                Player player = entry.getKey();
                int invested = entry.getValue();
                if (invested >= level) {
                    potSlice += level - previousLevel;
                    if (!player.isFolded() && player.isActive()) {
                        eligiblePlayers.add(player);
                    }
                } else if (invested > previousLevel) {
                    potSlice += invested - previousLevel;
                }
            }
            previousLevel = level;
            if (potSlice > 0 && !eligiblePlayers.isEmpty()) {
                layers.add(new SidePotLayer(potSlice, eligiblePlayers));
            }
        }
        return layers;
    }

    private void awardSidePotLayer(
            SidePotLayer layer,
            Map<Player, HandValue> playerResults,
            Map<Player, Integer> chipsWonByPlayer
    ) {
        List<Player> winners = new ArrayList<>();
        HandValue bestValue = null;
        for (Player player : layer.eligiblePlayers()) {
            HandValue handValue = playerResults.get(player);
            if (handValue == null) {
                continue;
            }
            if (bestValue == null || handValue.compareTo(bestValue) > 0) {
                bestValue = handValue;
                winners.clear();
                winners.add(player);
            } else if (handValue.compareTo(bestValue) == 0) {
                winners.add(player);
            }
        }
        if (winners.isEmpty()) {
            return;
        }
        int share = layer.amount() / winners.size();
        for (Player winner : winners) {
            chipsWonByPlayer.merge(winner, share, Integer::sum);
        }
    }

    private HandShowdownResult buildShowdownResultWithWinnings(
            int potTotal,
            String reason,
            Map<Player, HandValue> playerHandValues,
            Map<Player, Integer> chipsWonByPlayer
    ) {
        List<HandShowdownResult.PlayerLine> lines = new ArrayList<>();
        for (int seatIndex = 0; seatIndex < table.getSeats().length; seatIndex++) {
            Player player = table.getSeats()[seatIndex];
            if (player == null || player.isFolded()) {
                continue;
            }
            int chipsWon = chipsWonByPlayer.getOrDefault(player, 0);
            List<String> holeCardCodes = new ArrayList<>();
            for (Card holeCard : player.getHoleCards()) {
                holeCardCodes.add(holeCard.toString());
            }
            String handTypeName = "";
            HandValue handValue = playerHandValues.get(player);
            if (handValue != null) {
                handTypeName = handValue.category().getDescription();
            }
            lines.add(new HandShowdownResult.PlayerLine(
                    seatIndex,
                    player.getUsername(),
                    holeCardCodes,
                    chipsWon,
                    chipsWon > 0,
                    handTypeName
            ));
        }
        return new HandShowdownResult(table.getRoomId(), potTotal, reason, lines);
    }

    private HandShowdownResult buildShowdownResult(
            int potTotal,
            String reason,
            List<Player> winners,
            int sharePerWinner,
            Map<Player, HandValue> playerHandValues
    ) {
        List<HandShowdownResult.PlayerLine> lines = new ArrayList<>();
        if ("fold".equals(reason)) {
            for (Player winner : winners) {
                lines.add(new HandShowdownResult.PlayerLine(
                        winner.getSeatIndex(),
                        winner.getUsername(),
                        List.of(),
                        sharePerWinner,
                        true,
                        ""
                ));
            }
            return new HandShowdownResult(table.getRoomId(), potTotal, reason, lines);
        }

        for (int seatIndex = 0; seatIndex < table.getSeats().length; seatIndex++) {
            Player player = table.getSeats()[seatIndex];
            if (player == null || player.isFolded()) {
                continue;
            }
            boolean isWinner = winners.contains(player);
            int chipsWon = isWinner ? sharePerWinner : 0;
            List<String> holeCardCodes = new ArrayList<>();
            for (Card holeCard : player.getHoleCards()) {
                holeCardCodes.add(holeCard.toString());
            }
            String handTypeName = "";
            if (playerHandValues != null) {
                HandValue handValue = playerHandValues.get(player);
                if (handValue != null) {
                    handTypeName = handValue.category().getDescription();
                }
            }
            lines.add(new HandShowdownResult.PlayerLine(
                    seatIndex,
                    player.getUsername(),
                    holeCardCodes,
                    chipsWon,
                    isWinner,
                    handTypeName
            ));
        }
        return new HandShowdownResult(table.getRoomId(), potTotal, reason, lines);
    }

    public void moveToNextDealer(){
        int nextDealer = getNextActiveSeat(table.getDealerIndex());
        table.setDealerIndex(nextDealer);

        int sb = getNextActiveSeat(nextDealer);
        int bb = getNextActiveSeat(sb);

       table.setSmallBlindIndex(sb);
       table.setBigBlindIndex(bb);
    }

    private void postBlinds(){
        int sbIdx = table.getSmallBlindIndex();
        int bbIdx = table.getBigBlindIndex();

        Player sbPlayer = table.getSeats()[sbIdx];
        Player bbPlayer = table.getSeats()[bbIdx];
        if (sbPlayer == null || bbPlayer == null){
            throw new IllegalStateException("盲位无玩家");
        }

        sbPlayer.betUpTo(smallBlind);
        bbPlayer.betUpTo(bigBlind);
        table.setCurrentMaxBet(Math.max(sbPlayer.getCurrentBet(), bbPlayer.getCurrentBet()));
        actedThisRound.clear();
    }

    private void dealHoleCards(){
        for (int i = 0; i<2; i++){
            //发俩次
            for (Player p : table.getSeats()){
                if (p != null && p.isActive()){
                    p.addCard(table.getDeck().deal());
                }
            }
        }
    }



    /** 当前下注轮是否所有人都跟平 maxBet（all-in 玩家 currentBet 可能小于 maxBet，也算完成） */
    private boolean isBettingRoundComplete() {
        int maxBet = table.getCurrentMaxBet();
        for (Player p : table.getSeats()) {
            if (!canAct(p)) {
                continue; // 弃牌 / all-in 跳过
            }
            if (p.getCurrentBet() < maxBet) {
                return false;
            }
        }
        return true;
    }

    /** 保留原逻辑：未弃牌即可（发牌、比牌、找 blind 位用） */
    private int getNextActiveSeat(int startIndex) {
        int maxSeats = table.getSeats().length;
        int curr = (startIndex + 1) % maxSeats;
        for (int i = 0; i < maxSeats; i++) {
            Player p = table.getSeats()[curr];
            if (p != null && p.isActive() && !p.isFolded()) {
                return curr;
            }
            curr = (curr + 1) % maxSeats;
        }
        return -1;
    }

        /**统计有筹码，在本局中active的玩家数*/
        private int countActivePlayers(){
            int count = 0;
            for (Player p : table.getSeats() ){
                if (p != null && p.isActive()){
                    count++;
                }
            }
            return count;
        }

        /**未弃牌且仍在居中的玩家（含ALLin）*/
        private int countPlayerInHand(){
            int count = 0;
            for (Player p : table.getSeats()){
                if (p != null && p.isActive() && !p.isFolded()){
                    count++;
                }
            }
            return count;
        }

    /** 还能做决策的玩家：未弃牌、未 all-in、有筹码 */
    private boolean canAct(Player p) {
        return p != null && p.isActive() && !p.isFolded() && !p.isAllIn();
    }
    /** 从 startIndex 的下一位起，找下一个还能行动的玩家 */
    private int getNextSeatCanAct(int startIndex) {
        int maxSeats = table.getSeats().length;
        int curr = (startIndex + 1) % maxSeats;
        for (int i = 0; i < maxSeats; i++) {
            if (canAct(table.getSeats()[curr])) {
                return curr;
            }
            curr = (curr + 1) % maxSeats;
        }
        return -1; // 无人能行动
    }
    private void validatePlayerTurn(int seatIndex){
        if (table.getCurrentTurnIndex() != seatIndex){
            throw new IllegalStateException("未轮到该座位操作");
        }
    }

    /** 全员无法行动时是否还需继续跑牌（公牌未满且尚未摊牌） */
    public boolean needsRunout() {
        if (pendingShowdown != null) {
            return false;
        }
        if (table.getCommunityCards().size() >= 5) {
            return false;
        }
        if (countPlayerInHand() <= 1) {
            return false;
        }
        return getNextSeatCanAct(table.getDealerIndex()) == -1;
    }

    /**
     * 跑牌：发一轮公牌；发满 5 张后摊牌。
     *
     * @return true 表示后续还有未发的街
     */
    public boolean advanceRunoutStreet() {
        if (table.getCommunityCards().size() >= 5) {
            if (pendingShowdown == null) {
                settleHand();
            }
            return false;
        }
        int need = table.getCommunityCards().isEmpty() ? 3 : 1;
        advanceStreet(need);
        if (table.getCommunityCards().size() >= 5) {
            settleHand();
            return false;
        }
        return true;
    }

    private void advanceToNextStreetOrShowdown(){
        int communitySize = table.getCommunityCards().size();
        if (communitySize == 0){
            advanceStreet(3); //flop
            lastAggressorIndex = table.getDealerIndex();
        }else if (communitySize == 3){
            advanceStreet(1); //turn
            lastAggressorIndex = table.getDealerIndex();
        } else if (communitySize == 4) {
            advanceStreet(1); //river
            lastAggressorIndex = table.getDealerIndex();
        }else {
            settleHand();
        }
    }

    private void checkRoundOrHandOver(int actedSeatIndex){
        //1.只剩1人-收池
        if (countPlayerInHand() <= 1){
            settleHand();
            return;
        }
       //2.能行动的人数为0（全员all-in或弃牌）-》 先跑翻牌，后续由调度器逐街发牌
        if (getNextSeatCanAct(actedSeatIndex) == -1){
            advanceRunoutStreet();
            return;
        }
        //3.下注轮结束：所有人跟平，且本街能行动的玩家都已行动
        if (isBettingRoundComplete() && allActedThisRound()) {
            advanceToNextStreetOrShowdown();
            return;
        }
        //4.下一个能行动的玩家
        int nextTurn = getNextSeatCanAct(actedSeatIndex);
        table.setCurrentTurnIndex(nextTurn);
    }

    /** 将各座位当前轮下注收入底池 */
    private void sweepBetsToPot() {
        for (Player player : table.getSeats()) {
            if (player != null && player.getCurrentBet() > 0) {
                table.addPot(player.getCurrentBet());
                player.clearCurrentBet();
            }
        }
    }

    private boolean allActedThisRound() {
        for (int seatIndex = 0; seatIndex < table.getSeats().length; seatIndex++) {
            Player player = table.getSeats()[seatIndex];
            if (canAct(player) && !actedThisRound.contains(seatIndex)) {
                return false;
            }
        }
        return true;
    }

    private int countSeatedPlayersWithChips() {
        int count = 0;
        for (Player player : table.getSeats()) {
            if (player != null && player.getChips() > 0) {
                count++;
            }
        }
        return count;
    }
}
