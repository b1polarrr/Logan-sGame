package com.mercury.poker.network;

import com.mercury.poker.engine.GameEngine;
import com.mercury.poker.engine.HandShowdownResult;
import com.mercury.poker.engine.redis.LobbyRoomMeta;
import com.mercury.poker.engine.redis.RedisRoomRegistry;
import com.mercury.poker.engine.redis.RedisSessionStore;
import com.mercury.poker.events.EventPublisher;
import com.mercury.poker.engine.model.Player;
import com.mercury.poker.engine.model.Table;
import com.mercury.poker.network.protocol.PlayerActionRequest;
import com.mercury.poker.network.protocol.GameType;
import com.mercury.poker.network.protocol.RoomInfo;
import com.mercury.poker.network.protocol.RoomListResponse;
import com.mercury.poker.network.protocol.ActionType;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 核心房间路由器（单例模式）
 * 负责在内存中管理所有活跃的扑克房间，并将网络指令精准路由到对应的房间状态机
 */
public class RoomRouter {
    private static final int DEFAULT_BUY_IN = 1000;
    private static final long EMPTY_ROOM_TIMEOUT_MS = 5 * 60 * 1000L;
    private static final long SHOWDOWN_DELAY_MS = 10000L;
    private static final long RUNOUT_STREET_DELAY_MS = 2000L;
    private static final ScheduledExecutorService HAND_SCHEDULER = Executors.newSingleThreadScheduledExecutor(handler -> {
        Thread thread = new Thread(handler, "poker-hand-scheduler");
        thread.setDaemon(true);
        return thread;
    });
    //线程安全的哈希表，防止多线程同时创建/销毁房间发生数据死锁
    private final ConcurrentHashMap<String, GameEngine> activeRoom = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, GameType> roomGameTypes = new ConcurrentHashMap<>();
    /** 房间无人坐下时记录的空闲起始时间 */
    private final ConcurrentHashMap<String, Long> emptySince = new ConcurrentHashMap<>();
    /** 防止同一房间重复调度跑牌 */
    private final ConcurrentHashMap<String, Boolean> runoutScheduled = new ConcurrentHashMap<>();

    //单例模式：饿汉式
    private static final RoomRouter INSTANCE = new RoomRouter();

    private RoomRouter(){
        System.out.println("♣♦ 房间路由器初始化成功...");
    }
    public static RoomRouter getInstance(){
        return INSTANCE;
    }

    /**
     * 核心路由分发方法
     */
    public void route(ChannelHandlerContext ctx, PlayerActionRequest request) {
        SessionManager sessionManager = SessionManager.getINSTANCE();
        PlayerSession session = sessionManager.getSession(ctx.channel());
        if (session == null) {
            return;
        }
        try {
            switch (request.getActionType()) {
                case CREATE_ROOM -> handleCreateRoom(ctx, session, request);
                case LIST_ROOMS -> handleListRooms(ctx);
                case RECONNECT -> handleReconnect(ctx, request);
                default -> routeRoomAction(ctx, request, session);
            }
        } catch (Exception exception) {
            System.err.println("指令处理失败: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    private void routeRoomAction(ChannelHandlerContext ctx, PlayerActionRequest request, PlayerSession session) {
        String roomId = request.getRoomId();
        GameEngine gameEngine = activeRoom.get(roomId);
        if (gameEngine == null) {
            if (request.getActionType() == ActionType.JOIN_ROOM) {
                handleMissingRoomJoin(ctx, roomId);
            } else {
                System.out.println("玩家尝试操作不存在的房间: " + roomId);
            }
            return;
        }
        switch (request.getActionType()) {
            case JOIN_ROOM -> handleJoinRoom(ctx, gameEngine, session, roomId);
            case SIT_DOWN -> handleSitDown(ctx, gameEngine, session, request.getSeatIndex());
            case FOLD -> handleFold(gameEngine, session);
            case CHECK -> handleCheck(gameEngine, session);
            case CALL -> handleCall(gameEngine, session);
            case RAISE -> handleRaise(gameEngine, session, request.getAmount());
            case REBUY -> handleRebuy(gameEngine, session, request.getAmount());
            case READY -> handleReady(gameEngine, session);
            case DECLINE_REBUY -> handleDeclineRebuy(gameEngine, session);
            default -> System.out.println("未支持的网络指令: " + request.getActionType());
        }
    }

    private void handleRaise(GameEngine gameEngine, PlayerSession session, int targetTotalBet) {
        requireSeated(session);
        int seatIndex = session.getSeatIndex();
        gameEngine.playerBet(seatIndex, targetTotalBet);
        System.out.println("座位 " + seatIndex + " 加注到 " + targetTotalBet);
        publishPlayerAction(session, "RAISE", seatIndex, targetTotalBet);
        broadcastSnapshot(gameEngine, session.getRoomId());
    }

    private void handleCall(GameEngine gameEngine, PlayerSession session) {
        requireSeated(session);
        int seatIndex = session.getSeatIndex();
        Player player = gameEngine.getTable().getSeats()[seatIndex];
        gameEngine.playerCall(seatIndex);
        int actualTotal = player.getCurrentBet();
        System.out.println("座位 " + seatIndex + " 跟注到 " + actualTotal
                + (player.isAllIn() ? "（全下）" : ""));
        publishPlayerAction(session, player.isAllIn() ? "ALL_IN" : "CALL", seatIndex, actualTotal);
        broadcastSnapshot(gameEngine, session.getRoomId());
    }

    private void handleCheck(GameEngine gameEngine, PlayerSession session) {
        requireSeated(session);
        int seatIndex = session.getSeatIndex();
        Player player = gameEngine.getTable().getSeats()[seatIndex];
        gameEngine.playerBet(seatIndex, player.getCurrentBet());
        System.out.println("座位 " + seatIndex + " 过牌");
        publishPlayerAction(session, "CHECK", seatIndex, player.getCurrentBet());
        broadcastSnapshot(gameEngine, session.getRoomId());
    }

    private void handleFold(GameEngine gameEngine, PlayerSession session) {
        requireSeated(session);
        int seatIndex = session.getSeatIndex();
        gameEngine.playerFold(seatIndex);
        System.out.println("座位 " + seatIndex + " 弃牌");
        publishPlayerAction(session, "FOLD", seatIndex, 0);
        broadcastSnapshot(gameEngine, session.getRoomId());
    }

    private void handleRebuy(GameEngine gameEngine, PlayerSession session, int amount) {
        requireSeated(session);
        int seatIndex = session.getSeatIndex();
        int rebuyAmount = amount > 0 ? amount : DEFAULT_BUY_IN;
        gameEngine.playerRebuy(seatIndex, rebuyAmount);
        System.out.println("座位 " + seatIndex + " 补充筹码 " + rebuyAmount);
        publishPlayerAction(session, "REBUY", seatIndex, rebuyAmount);
        tryStartNextHandIfEligible(gameEngine, session.getRoomId());
        broadcastSnapshot(gameEngine, session.getRoomId());
    }

    private void handleDeclineRebuy(GameEngine gameEngine, PlayerSession session) {
        requireSeated(session);
        int seatIndex = session.getSeatIndex();
        gameEngine.playerDeclineRebuy(seatIndex);
        System.out.println("座位 " + seatIndex + " 选择稍后再说（不补码）");
        publishPlayerAction(session, "DECLINE_REBUY", seatIndex, 0);
        tryStartNextHandIfEligible(gameEngine, session.getRoomId());
        broadcastSnapshot(gameEngine, session.getRoomId());
    }

    private void handleReady(GameEngine gameEngine, PlayerSession session) {
        requireSeated(session);
        if (!gameEngine.canStartNewHand()) {
            throw new IllegalStateException("当前无法准备");
        }
        int seatIndex = session.getSeatIndex();
        Player player = gameEngine.getTable().getSeats()[seatIndex];
        if (player.getChips() <= 0) {
            throw new IllegalStateException("筹码不足，请先补充筹码");
        }
        player.setReady(true);
        System.out.println("座位 " + seatIndex + " 已准备");
        publishEvent("player_ready", Map.of(
                "roomId", session.getRoomId(),
                "userId", session.getUserId(),
                "username", session.getUsername(),
                "seatIndex", String.valueOf(seatIndex)
        ));
        tryStartHandIfAllReady(gameEngine, session.getRoomId());
        broadcastSnapshot(gameEngine, session.getRoomId());
    }

    private void handleSitDown(ChannelHandlerContext ctx, GameEngine gameEngine, PlayerSession session, int seatIndex) {
        requireJoined(session);
        if (session.getSeatIndex() >= 0) {
            throw new IllegalStateException("你已经坐下，座位号: " + session.getSeatIndex());
        }
        Table table = gameEngine.getTable();
        if (seatIndex < 0 || seatIndex >= table.getSeats().length) {
            throw new IllegalArgumentException("非法座位号: " + seatIndex);
        }
        if (table.getSeats()[seatIndex] != null) {
            throw new IllegalStateException("座位 " + seatIndex + " 已有人");
        }
        Player player = new Player(session.getUserId(), session.getUsername(), DEFAULT_BUY_IN);
        table.sitDown(player, seatIndex);
        SessionManager.getINSTANCE().sitDown(ctx.channel(), seatIndex);
        clearAllReady(table);
        System.out.println("玩家 " + session.getUsername() + " 坐到座位 " + seatIndex);
        publishEvent("player_sat", Map.of(
                "roomId", table.getRoomId(),
                "userId", session.getUserId(),
                "username", session.getUsername(),
                "seatIndex", String.valueOf(seatIndex)
        ));
        markEmptyState(table.getRoomId(), gameEngine);
        RedisRoomRegistry.getINSTANCE().updateSeatedCount(
                table.getRoomId(),
                countSeatedPlayers(table)
        );
        broadcastSnapshot(gameEngine, table.getRoomId());
    }

    private void handleListRooms(ChannelHandlerContext ctx) {
        cleanupExpiredEmptyRooms();
        RoomListResponse.Builder responseBuilder = RoomListResponse.newBuilder();
        for (LobbyRoomMeta lobbyRoomMeta : RedisRoomRegistry.getINSTANCE().listAllRooms()) {
            String roomId = lobbyRoomMeta.getRoomId();
            GameEngine gameEngine = activeRoom.get(roomId);
            if (gameEngine == null) {
                continue;
            }
            GameType gameType = roomGameTypes.getOrDefault(roomId, GameType.TEXAS_HOLDEM);
            responseBuilder.addRooms(buildRoomInfo(roomId, gameEngine, gameType));
        }
        SnapshotBroadcaster.getINSTANCE().sendRoomList(ctx.channel(), responseBuilder.build());
        System.out.println("已发送房间列表，共 " + responseBuilder.getRoomsCount() + " 个房间");
    }

    private void handleCreateRoom(ChannelHandlerContext ctx, PlayerSession session, PlayerActionRequest request) {
        GameType gameType = request.getGameType();
        if (gameType != GameType.TEXAS_HOLDEM) {
            throw new IllegalArgumentException("暂仅支持德州扑克");
        }
        int maxSeats = request.getMaxSeats() > 0 ? request.getMaxSeats() : 6;
        int smallBlind = request.getSmallBlind() > 0 ? request.getSmallBlind() : 10;
        int bigBlind = request.getBigBlind() > 0 ? request.getBigBlind() : 20;
        String roomId = generateRoomId();
        Table table = new Table(roomId, maxSeats);
        GameEngine gameEngine = GameEngineFactory.create(gameType, table, smallBlind, bigBlind);
        activeRoom.put(roomId, gameEngine);
        roomGameTypes.put(roomId, gameType);
        markEmptyState(roomId, gameEngine);
        SessionManager.getINSTANCE().joinRoom(ctx.channel(), roomId);
        RoomInfo roomInfo = buildRoomInfo(roomId, gameEngine, gameType);
        RedisRoomRegistry.getINSTANCE().registerRoom(toLobbyRoomMeta(roomInfo));
        SnapshotBroadcaster.getINSTANCE().sendRoomCreated(ctx.channel(), roomInfo);
        publishEvent("room_created", Map.of(
                "roomId", roomId,
                "userId", session.getUserId(),
                "username", session.getUsername(),
                "gameType", gameType.name()
        ));
        System.out.println("玩家 " + session.getUsername() + " 创建并加入房间 " + roomId);
    }

    private void handleJoinRoom(ChannelHandlerContext ctx, GameEngine gameEngine, PlayerSession session, String roomId) {
        SessionManager.getINSTANCE().joinRoom(ctx.channel(), roomId);
        publishEvent("room_joined", Map.of(
                "roomId", roomId,
                "userId", session.getUserId(),
                "username", session.getUsername()
        ));
        System.out.println("玩家 " + session.getUsername() + "加入房间" + session.getRoomId());
        broadcastSnapshot(gameEngine, roomId);
    }

    private void handleMissingRoomJoin(ChannelHandlerContext ctx, String roomId) {
        RedisRoomRegistry roomRegistry = RedisRoomRegistry.getINSTANCE();
        if (!roomRegistry.roomExists(roomId)) {
            SnapshotBroadcaster.getINSTANCE().sendError(
                    ctx.channel(),
                    "ROOM_NOT_FOUND",
                    "房间不存在或已关闭"
            );
            System.out.println("加入失败：房间不存在 " + roomId);
            return;
        }
        String roomPodId = roomRegistry.getRoomPodId(roomId);
        String currentPodId = PodIdentity.getPodName();
        if (roomPodId != null && !roomPodId.equals(currentPodId)) {
            SnapshotBroadcaster.getINSTANCE().sendError(
                    ctx.channel(),
                    "ROOM_ON_OTHER_POD",
                    "房间在另一台服务器（" + roomPodId + "）上，请刷新页面重试"
            );
            System.out.println("加入失败：房间在其他 Pod " + roomPodId + "，当前 " + currentPodId);
            return;
        }
        SnapshotBroadcaster.getINSTANCE().sendError(
                ctx.channel(),
                "ROOM_NOT_AVAILABLE",
                "房间暂不可用，请稍后重试"
        );
        System.out.println("加入失败：房间元数据存在但本 Pod 无内存房间 " + roomId);
    }

    private void publishPlayerAction(PlayerSession session, String action, int seatIndex, int amount) {
        publishEvent("player_action", Map.of(
                "roomId", session.getRoomId() == null ? "" : session.getRoomId(),
                "userId", session.getUserId(),
                "username", session.getUsername(),
                "action", action,
                "seatIndex", String.valueOf(seatIndex),
                "amount", String.valueOf(amount)
        ));
    }

    private void publishEvent(String eventType, Map<String, String> fields) {
        EventPublisher.getINSTANCE().publish(eventType, fields);
    }

    /**
     * 断线重连：从 Redis 恢复玩家身份与房间/座位，重新订阅房间快照。
     */
    private void handleReconnect(ChannelHandlerContext ctx, PlayerActionRequest request) {
        String sessionToken = request.getSessionToken();
        if (sessionToken == null || sessionToken.isBlank()) {
            System.out.println("重连失败：未提供 session_token");
            return;
        }

        PlayerSession ephemeralSession = SessionManager.getINSTANCE().getSession(ctx.channel());
        String ephemeralToken = ephemeralSession != null ? ephemeralSession.getSessionToken() : null;
        if (ephemeralToken != null && !ephemeralToken.equals(sessionToken)) {
            RedisSessionStore.getINSTANCE().delete(ephemeralToken);
        }

        Map<String, String> sessionData = RedisSessionStore.getINSTANCE().load(sessionToken);
        if (sessionData.isEmpty()) {
            System.out.println("重连失败：token 无效或已过期");
            return;
        }

        String userId = sessionData.getOrDefault("userId", "");
        String username = sessionData.getOrDefault("username", "");
        String roomId = sessionData.getOrDefault("roomId", "");
        if (roomId.isBlank()) {
            roomId = null;
        }
        int seatIndex = parseSeatIndex(sessionData.get("seatIndex"));

        SessionManager.getINSTANCE().restoreSession(
                ctx.channel(),
                sessionToken,
                userId,
                username,
                roomId,
                seatIndex
        );
        SnapshotBroadcaster.getINSTANCE().sendSessionConnected(ctx.channel(), sessionToken);

        if (roomId == null) {
            handleListRooms(ctx);
            System.out.println("玩家 " + username + " 重连成功（大厅）");
            return;
        }

        GameEngine gameEngine = activeRoom.get(roomId);
        if (gameEngine == null) {
            System.out.println("重连：房间 " + roomId + " 已不存在，返回大厅");
            SessionManager.getINSTANCE().clearRoom(ctx.channel());
            handleListRooms(ctx);
            return;
        }

        if (seatIndex >= 0) {
            Table table = gameEngine.getTable();
            if (seatIndex < table.getSeats().length) {
                Player player = table.getSeats()[seatIndex];
                if (player != null && player.getUserId().equals(userId)) {
                    player.setOnline(true);
                }
            }
        }

        broadcastSnapshot(gameEngine, roomId);
        System.out.println("玩家 " + username + " 重连成功，房间 " + roomId + " 座位 " + seatIndex);
    }

    /** 玩家断线时标记桌上座位离线并广播快照 */
    public void markPlayerOffline(String userId, String roomId) {
        GameEngine gameEngine = activeRoom.get(roomId);
        if (gameEngine == null) {
            return;
        }
        Table table = gameEngine.getTable();
        for (Player player : table.getSeats()) {
            if (player != null && player.getUserId().equals(userId)) {
                player.setOnline(false);
                broadcastSnapshot(gameEngine, roomId);
                return;
            }
        }
    }

    private int parseSeatIndex(String rawSeatIndex) {
        if (rawSeatIndex == null || rawSeatIndex.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(rawSeatIndex.trim());
        } catch (NumberFormatException numberFormatException) {
            return -1;
        }
    }

    private void broadcastSnapshot(GameEngine gameEngine, String roomId) {
        HandShowdownResult showdownResult = gameEngine.consumePendingShowdown();
        if (showdownResult != null) {
            SnapshotBroadcaster.getINSTANCE().broadcastShowdownReveal(gameEngine, roomId, showdownResult);
            SnapshotBroadcaster.getINSTANCE().sendShowdown(roomId, showdownResult);
            scheduleNextHand(roomId);
            System.out.println(
                    "摊牌结算：房间 " + roomId
                            + " 方式 " + showdownResult.getReason()
                            + " 底池 " + showdownResult.getPotTotal()
            );
        } else {
            SnapshotBroadcaster.getINSTANCE().broadcast(gameEngine, roomId);
            if (gameEngine.needsRunout()) {
                scheduleRunoutStreet(roomId);
            } else if (gameEngine.needsDelayedRunoutShowdown()) {
                scheduleRunoutShowdown(roomId);
            } else {
                runoutScheduled.remove(roomId);
            }
        }
    }

    /** all-in 后逐街发公牌：翻牌 → 等 2s → 转牌 → 等 2s → 河牌 → 等 2s → 摊牌 */
    private void scheduleRunoutStreet(String roomId) {
        if (runoutScheduled.putIfAbsent(roomId, Boolean.TRUE) != null) {
            return;
        }
        HAND_SCHEDULER.schedule(() -> {
            runoutScheduled.remove(roomId);
            try {
                GameEngine engine = activeRoom.get(roomId);
                if (engine == null) {
                    return;
                }
                engine.advanceRunoutStreet();
                broadcastSnapshot(engine, roomId);
            } catch (Exception exception) {
                System.err.println("跑牌失败: " + exception.getMessage());
                exception.printStackTrace();
            }
        }, RUNOUT_STREET_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /** 河牌发完后延迟 2 秒再摊牌（河牌阶段 all-in 不走此路径） */
    private void scheduleRunoutShowdown(String roomId) {
        if (runoutScheduled.putIfAbsent(roomId, Boolean.TRUE) != null) {
            return;
        }
        HAND_SCHEDULER.schedule(() -> {
            runoutScheduled.remove(roomId);
            try {
                GameEngine engine = activeRoom.get(roomId);
                if (engine == null) {
                    return;
                }
                engine.settleRunoutHand();
                broadcastSnapshot(engine, roomId);
            } catch (Exception exception) {
                System.err.println("跑牌摊牌失败: " + exception.getMessage());
                exception.printStackTrace();
            }
        }, RUNOUT_STREET_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void scheduleNextHand(String roomId) {
        HAND_SCHEDULER.schedule(() -> {
            try {
                GameEngine gameEngine = activeRoom.get(roomId);
                if (gameEngine == null) {
                    return;
                }
                if (tryStartNextHandIfEligible(gameEngine, roomId)) {
                    SnapshotBroadcaster.getINSTANCE().broadcast(gameEngine, roomId);
                    System.out.println("房间 " + roomId + " 已开始下一局");
                } else if (gameEngine.canStartNewHand()) {
                    SnapshotBroadcaster.getINSTANCE().broadcast(gameEngine, roomId);
                    System.out.println("房间 " + roomId + " 等待玩家补码或稍后再说后再开局");
                } else {
                    System.out.println("房间 " + roomId + " 暂无法开局，等待至少 2 名玩家有筹码");
                }
            } catch (Exception exception) {
                System.err.println("开始下一局失败: " + exception.getMessage());
                exception.printStackTrace();
            }
        }, SHOWDOWN_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /** 局间：至少 2 名玩家有筹码，且无人待询问补码时开下一局 */
    private boolean tryStartNextHandIfEligible(GameEngine gameEngine, String roomId) {
        if (!gameEngine.canStartNewHand()) {
            return false;
        }
        if (countPlayersWithChips(gameEngine.getTable()) < 2) {
            return false;
        }
        if (hasPlayersAwaitingRebuyPrompt(gameEngine.getTable())) {
            return false;
        }
        clearAllReady(gameEngine.getTable());
        if (!gameEngine.startNextHand()) {
            return false;
        }
        publishEvent("hand_started", Map.of("roomId", roomId));
        return true;
    }

    /** 筹码为 0 且仍待询问补码的玩家会阻塞下一局开局 */
    private boolean hasPlayersAwaitingRebuyPrompt(Table table) {
        for (Player player : table.getSeats()) {
            if (player != null && player.getChips() == 0 && player.isWillRebuy()) {
                return true;
            }
        }
        return false;
    }

    private int countPlayersWithChips(Table table) {
        int count = 0;
        for (Player player : table.getSeats()) {
            if (player != null && player.getChips() > 0) {
                count++;
            }
        }
        return count;
    }

    private void requireJoined(PlayerSession session) {
        if (session.getRoomId() == null) {
            throw new IllegalStateException("请先加入房间");
        }
    }

    private void requireSeated(PlayerSession session) {
        requireJoined(session);
        if (session.getSeatIndex() < 0) {
            throw new IllegalStateException("请先坐下");
        }
    }

    private int countSeatedPlayers(Table table) {
        int count = 0;
        for (Player player : table.getSeats()) {
            if (player != null) {
                count++;
            }
        }
        return count;
    }

    private void clearAllReady(Table table) {
        for (Player player : table.getSeats()) {
            if (player != null) {
                player.setReady(false);
            }
        }
    }

    private boolean areAllPlayersWithChipsReady(Table table) {
        int playersWithChips = 0;
        for (Player player : table.getSeats()) {
            if (player == null || player.getChips() <= 0) {
                continue;
            }
            playersWithChips++;
            if (!player.isReady()) {
                return false;
            }
        }
        return playersWithChips >= 2 && !hasPlayersAwaitingRebuyPrompt(table);
    }

    private void tryStartHandIfAllReady(GameEngine gameEngine, String roomId) {
        Table table = gameEngine.getTable();
        if (!gameEngine.canStartNewHand() || !areAllPlayersWithChipsReady(table)) {
            return;
        }
        clearAllReady(table);
        gameEngine.startNewHand();
        System.out.println("房间 " + roomId + " 全员准备，开始游戏");
        publishEvent("hand_started", Map.of("roomId", roomId));
    }

    private RoomInfo buildRoomInfo(String roomId, GameEngine gameEngine, GameType gameType) {
        Table table = gameEngine.getTable();
        return RoomInfo.newBuilder()
                .setRoomId(roomId)
                .setGameType(gameType)
                .setMaxSeats(table.getSeats().length)
                .setSeatedCount(countSeatedPlayers(table))
                .setSmallBlind(gameEngine.getSmallBlind())
                .setBigBlind(gameEngine.getBigBlind())
                .build();
    }

    private LobbyRoomMeta toLobbyRoomMeta(RoomInfo roomInfo) {
        return new LobbyRoomMeta(
                roomInfo.getRoomId(),
                roomInfo.getGameType().getNumber(),
                roomInfo.getMaxSeats(),
                roomInfo.getSeatedCount(),
                roomInfo.getSmallBlind(),
                roomInfo.getBigBlind()
        );
    }

    private RoomInfo toRoomInfo(LobbyRoomMeta lobbyRoomMeta) {
        GameType gameType = GameType.forNumber(lobbyRoomMeta.getGameType());
        if (gameType == null) {
            gameType = GameType.TEXAS_HOLDEM;
        }
        return RoomInfo.newBuilder()
                .setRoomId(lobbyRoomMeta.getRoomId())
                .setGameType(gameType)
                .setMaxSeats(lobbyRoomMeta.getMaxSeats())
                .setSeatedCount(lobbyRoomMeta.getSeatedCount())
                .setSmallBlind(lobbyRoomMeta.getSmallBlind())
                .setBigBlind(lobbyRoomMeta.getBigBlind())
                .build();
    }

    private String generateRoomId() {
        String roomId;
        do {
            roomId = String.valueOf(System.currentTimeMillis() % 1_000_000);
        } while (activeRoom.containsKey(roomId));
        return roomId;
    }

    private void markEmptyState(String roomId, GameEngine gameEngine) {
        if (countSeatedPlayers(gameEngine.getTable()) == 0) {
            emptySince.putIfAbsent(roomId, System.currentTimeMillis());
        } else {
            emptySince.remove(roomId);
        }
    }

    private void cleanupExpiredEmptyRooms() {
        long now = System.currentTimeMillis();
        for (String roomId : activeRoom.keySet()) {
            GameEngine gameEngine = activeRoom.get(roomId);
            if (gameEngine == null) {
                continue;
            }
            if (countSeatedPlayers(gameEngine.getTable()) > 0) {
                emptySince.remove(roomId);
                continue;
            }
            Long idleSince = emptySince.get(roomId);
            if (idleSince == null) {
                emptySince.put(roomId, now);
                continue;
            }
            if (now - idleSince >= EMPTY_ROOM_TIMEOUT_MS) {
                destroyRoom(roomId);
            }
        }
    }

    private void destroyRoom(String roomId) {
        activeRoom.remove(roomId);
        roomGameTypes.remove(roomId);
        emptySince.remove(roomId);
        RedisRoomRegistry.getINSTANCE().unregisterRoom(roomId);
        System.out.println("空房超时销毁房间: " + roomId);
    }
}
