package com.router;

import com.core.messages.AcceptConnection;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.util.HashMap;

public class Router implements Runnable{

    private static HashMap<Integer, ChannelHandlerContext> routeTable = new HashMap<>();
    private String clientType;
    private final int port;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public static void main (String [] args)
            throws Exception {

        Router brokerServer = new Router(5000);
        Thread brokerServerThread = new Thread(brokerServer);
        brokerServerThread.start();

        Router marketServer = new Router(5001);
        Thread marketServerThread = new Thread(marketServer);
        marketServerThread.start();
    }

    public Router(int port) {
        this.port = port;
        clientType = port == 5000 ? "Broker" : "Market";
    }

    @Override
    public void run()
    {
        //accepts new connections as they arrive and pass them to worker for proccessing
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            //determines how to server will proccess incoming connections
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>(){
                        @Override
                        public void initChannel (SocketChannel ch) throws Exception
                        {
                            ChannelPipeline pipeline = ch.pipeline();

                            pipeline.addLast("decoder", new StringDecoder());
                            pipeline.addLast("encoder", new StringEncoder());
                            pipeline.addLast("handler", new ProcessingHandler());
                        }
                    }).option(ChannelOption.SO_REUSEADDR, true);

            //bind server to port and start listening for incoming connections
            ChannelFuture f = bootstrap.bind(port).sync();
            f.channel().closeFuture().sync();
        }
        catch (InterruptedException e) { e.printStackTrace(); }
        finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    class ProcessingHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead (ChannelHandlerContext ctx, Object msg) {
            System.out.println(msg + " from " + ctx.channel().remoteAddress());
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
            System.out.println(clientType + " [" + ctx.channel().remoteAddress() + "] added");
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
            System.out.println(clientType + " [" + ctx.channel().remoteAddress() + "] removed");
        }

    }


    private void acceptNewConnection(ChannelHandlerContext ctx, Object msg) {
        AcceptConnection connectionMessage = (AcceptConnection) msg;
        String id = ctx.channel().remoteAddress().toString().substring(11);
        //noinspection UnusedAssignment
        id = id.concat(clientType.equals("Market") ? "0" : "1");
        connectionMessage.setId(Integer.valueOf(id));
        connectionMessage.setNewChecksum();
        ctx.writeAndFlush(connectionMessage);
        routeTable.put(connectionMessage.getId(), ctx);
        System.out.println("Accepted connection and assigned " + clientType + " to " + connectionMessage.getId());
    }
}
