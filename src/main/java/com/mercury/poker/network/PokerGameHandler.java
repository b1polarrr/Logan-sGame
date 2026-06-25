package com.mercury.poker.network;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mercury.poker.network.protocol.PlayerActionRequest;
import com.mercury.poker.network.SnapshotBroadcaster;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

/**
 * 核心网络业务处理器
 * 处理来自前那段的WebSocket二进制（protobuf）数据包
 */
public class PokerGameHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {

    /**
     * 当有新的玩家浏览器成功建立长连接时触发
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        PlayerSession playerSession = SessionManager.getINSTANCE().onConnect(ctx.channel());
        SnapshotBroadcaster.getINSTANCE().sendSessionConnected(
                ctx.channel(),
                playerSession.getSessionToken()
        );
        System.out.println("【网络通知】玩家连接成功，sessionToken=" + playerSession.getSessionToken());
    }

    /**
     *当玩家关闭网页，网络抖动断开连接时触发
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception{
        SessionManager.getINSTANCE().onDisconnect(ctx.channel());
        System.out.println("【网络通知】一位玩家断开连接，通道ID：" + ctx.channel());
    }

    /**
     * 收到前端发来的二进制数据包时触发
     * @param channelHandlerContext
     * @param binaryWebSocketFrame
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, BinaryWebSocketFrame binaryWebSocketFrame) throws Exception {
        //1.从WebSocket帧中提取原生的二进制字节数组
        byte[] bytes = new byte[binaryWebSocketFrame.content().readableBytes()];
        binaryWebSocketFrame.content().readBytes(bytes);

        try {
            //2.利用之前自动生成的protobuf代码，直接反序列化成Java对象
            PlayerActionRequest request = PlayerActionRequest.parseFrom(bytes);

            //3.打印测试，检查前端发来了什么指令
            System.out.println("【收到玩家指令】 操作类型: " + request.getActionType()
                    + ", 房间号: " + request.getRoomId()
                    + ", 座位号: " + request.getSeatIndex()
                    + ", 金额: " + request.getAmount());

            // 核心修改：直接单例调用房间路由器，把指令派发到对应房间！
            RoomRouter.getInstance().route(channelHandlerContext, request);
        } catch (Exception e) {
            System.err.println("【解析失败】收到未知格式的数据包: " + e.getMessage());
            e.printStackTrace();
        }
    }

        /**
         * 捕捉异常
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
            System.err.println(cause.getMessage());
            ctx.close();
        }

}
