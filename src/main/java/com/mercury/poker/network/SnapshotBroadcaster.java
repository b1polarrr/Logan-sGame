package com.mercury.poker.network;

import com.mercury.poker.engine.GameEngine;
import com.mercury.poker.engine.HandShowdownResult;
import com.mercury.poker.engine.model.Card;
import com.mercury.poker.engine.model.Player;
import com.mercury.poker.engine.model.Table;
import com.mercury.poker.network.protocol.*;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.util.HashSet;
import java.util.Set;

public class SnapshotBroadcaster {
    private static final SnapshotBroadcaster INSTANCE = new SnapshotBroadcaster();

    private SnapshotBroadcaster(){

    }

    public static SnapshotBroadcaster getINSTANCE() {
        return INSTANCE;
    }
    public TableSnapshotResponse buildSnapshot(
            GameEngine gameEngine,
            String roomId,
            String viewerUserId,
            Set<Integer> revealSeatIndices
    ){
        Table table = gameEngine.getTable();

        TableSnapshotResponse.Builder responseBuilder = TableSnapshotResponse.newBuilder()
                .setRoomId(roomId)
                .setPot(table.getPot())
                .setCurrentMaxBet(table.getCurrentMaxBet())
                .setDealerIndex(table.getDealerIndex())
                .setCurrentTurnIndex(table.getCurrentTurnIndex());

        //公牌
        for (Card communityCard : table.getCommunityCards()){
            responseBuilder.addCommunityCards(communityCard.toString());
        }

        //每位玩家
        Player[] seats = table.getSeats();
        for (int seatIndex = 0; seatIndex < seats.length; seatIndex++){
            Player player = seats[seatIndex];
            if (player == null){
                continue;
            }

            PlayerState.Builder playerStateBuilder = PlayerState.newBuilder()
                    .setUserId(player.getUserId())
                    .setUsername(player.getUsername())
                    .setSeatIndex(seatIndex)
                    .setChips(player.getChips())
                    .setCurrentBet(player.getCurrentBet())
                    .setIsFolded(player.isFolded())
                    .setIsAllIn(player.isAllIn())
                    .setIsOnline(player.isOnline())
                    .setSessionProfit(player.getSessionProfit())
                    .setIsReady(player.isReady())
                    .setWillRebuy(player.isWillRebuy());

            boolean revealHoleCards = player.getUserId().equals(viewerUserId)
                    || (revealSeatIndices != null && revealSeatIndices.contains(seatIndex));
            if (revealHoleCards) {
                for (Card holeCard : player.getHoleCards()){
                    playerStateBuilder.addHoleCards(holeCard.toString());
                }
            }
            responseBuilder.addPlayers(playerStateBuilder);
        }
        return responseBuilder.build();
    }

    public void sendRoomList(Channel channel, RoomListResponse roomListResponse) {
        ServerMessage serverMessage = ServerMessage.newBuilder()
                .setRoomList(roomListResponse)
                .build();
        sendServerMessage(channel, serverMessage);
    }

    public void sendRoomCreated(Channel channel, RoomInfo roomInfo) {
        ServerMessage serverMessage = ServerMessage.newBuilder()
                .setRoomCreated(roomInfo)
                .build();
        sendServerMessage(channel, serverMessage);
    }

    private void sendServerMessage(Channel channel, ServerMessage serverMessage) {
        if (channel == null || !channel.isActive()) {
            return;
        }
        channel.writeAndFlush(
                new BinaryWebSocketFrame(Unpooled.wrappedBuffer(serverMessage.toByteArray()))
        );
    }

    public void broadcast(GameEngine gameEngine, String roomId){
        broadcast(gameEngine, roomId, null);
    }

    public void broadcastShowdownReveal(GameEngine gameEngine, String roomId, HandShowdownResult result) {
        Set<Integer> revealSeatIndices = new HashSet<>();
        if ("showdown".equals(result.getReason())) {
            for (HandShowdownResult.PlayerLine playerLine : result.getPlayers()) {
                revealSeatIndices.add(playerLine.getSeatIndex());
            }
        }
        broadcast(gameEngine, roomId, revealSeatIndices);
    }

    public void broadcast(GameEngine gameEngine, String roomId, Set<Integer> revealSeatIndices){
        SessionManager sessionManager = SessionManager.getINSTANCE();

        for (Channel channel : sessionManager.getRoomChannels(roomId)){
            if (channel == null || !channel.isActive()){
                continue;
            }
            PlayerSession playerSession = sessionManager.getSession(channel);
            String viewerUserId = playerSession != null ? playerSession.getUserId() : "";

            TableSnapshotResponse snapshot = buildSnapshot(
                    gameEngine,
                    roomId,
                    viewerUserId,
                    revealSeatIndices
            );
            ServerMessage serverMessage = ServerMessage.newBuilder()
                    .setTableSnapshot(snapshot)
                    .build();
            sendServerMessage(channel, serverMessage);
        }
    }

    public void sendShowdown(String roomId, HandShowdownResult handShowdownResult) {
        ShowdownResult.Builder showdownBuilder = ShowdownResult.newBuilder()
                .setRoomId(handShowdownResult.getRoomId())
                .setPotTotal(handShowdownResult.getPotTotal())
                .setReason(handShowdownResult.getReason());
        for (HandShowdownResult.PlayerLine playerLine : handShowdownResult.getPlayers()) {
            showdownBuilder.addPlayers(ShowdownPlayerResult.newBuilder()
                    .setSeatIndex(playerLine.getSeatIndex())
                    .setUsername(playerLine.getUsername())
                    .addAllHoleCards(playerLine.getHoleCards())
                    .setChipsWon(playerLine.getChipsWon())
                    .setIsWinner(playerLine.isWinner())
                    .setHandType(playerLine.getHandTypeName())
                    .build());
        }
        ServerMessage serverMessage = ServerMessage.newBuilder()
                .setShowdown(showdownBuilder.build())
                .build();
        SessionManager sessionManager = SessionManager.getINSTANCE();
        for (Channel channel : sessionManager.getRoomChannels(roomId)) {
            sendServerMessage(channel, serverMessage);
        }
    }

    public void sendSessionConnected(Channel channel, String sessionToken){
        SessionConnectedResponse sessionConnected = SessionConnectedResponse.newBuilder()
                .setSessionToken(sessionToken)
                .build();
        ServerMessage serverMessage = ServerMessage.newBuilder()
                .setSessionConnected(sessionConnected)
                .build();
        sendServerMessage(channel, serverMessage);
    }

    public void sendError(Channel channel, String code, String message) {
        ErrorResponse errorResponse = ErrorResponse.newBuilder()
                .setCode(code)
                .setMessage(message)
                .build();
        ServerMessage serverMessage = ServerMessage.newBuilder()
                .setError(errorResponse)
                .build();
        sendServerMessage(channel, serverMessage);
    }
}
