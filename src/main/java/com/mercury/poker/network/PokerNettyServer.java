package com.mercury.poker.network;

import com.mercury.poker.events.EventPublisher;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class PokerNettyServer {
    private final int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private volatile ChannelFuture serverChannelFuture;

    public PokerNettyServer(int port){
        this.port = port;
    }

    public void start() throws InterruptedException{
        //1.创建Reactor线程组
        //BossGroup专门负责接收客户端的连接请求
        bossGroup = new NioEventLoopGroup(1);
        //WorkerGroup专门负责网络读写与业务逻辑处理
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            ChannelPipeline pipeline = socketChannel.pipeline();

                            //webSocket建立在HTTP协议之上，所以先配置HTTP编解码器
                            pipeline.addLast(new HttpServerCodec());
                            //以块传输方式支持大数据流写入
                            pipeline.addLast(new ChunkedWriteHandler());
                            //将HTTP消息的多个部分聚合成一个完整的FullHttpRequest
                            pipeline.addLast(new HttpObjectAggregator(65536));

                            //核心:配置webSocket协议处理器，指定路由为“/ws”
                            //自动处理握手、ping/pong心跳，以及长连接的升级
                            pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));

                            // 必须注册：负责连接通知与 protobuf 二进制指令解析
                            pipeline.addLast(new PokerGameHandler());
                        }
                    });
            // 启动时加载房间路由器，确保控制台先打印初始化日志
            RoomRouter.getInstance();

            registerShutdownHook();

            //2.绑定端口并同步等待服务器启动完成
            System.out.println("网关启动，监听端口: " + PokerNettyServer.this.port);
            serverChannelFuture = serverBootstrap.bind(port).sync();

            //3.阻塞等待服务器通道关闭（使得主线程不退出）
            serverChannelFuture.channel().closeFuture().sync();
        } finally {
            shutdownGracefully();
        }
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("收到 SIGTERM/SIGINT，开始优雅下线（draining）...");
            try {
                if (serverChannelFuture != null && serverChannelFuture.channel().isOpen()) {
                    serverChannelFuture.channel().close().sync();
                }
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                System.err.println("优雅下线被中断: " + interruptedException.getMessage());
            }
        }, "poker-graceful-shutdown"));
    }

    private void shutdownGracefully() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        EventPublisher.getINSTANCE().shutdown();
        System.out.println("Netty游戏网关已关闭");
    }

    public static void main(String[] args) throws InterruptedException{
        int port = resolvePort();
        System.out.println("POD_NAME=" + PodIdentity.getPodName() + " PORT=" + port);
        new PokerNettyServer(port).start();
    }

    private static int resolvePort() {
        String portEnvironment = System.getenv("PORT");
        if (portEnvironment == null || portEnvironment.isBlank()) {
            return 8888;
        }
        return Integer.parseInt(portEnvironment.trim());
    }
}
