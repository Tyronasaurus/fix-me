package com.core;

import com.core.messages.AcceptConnection;
import com.core.messages.FIXMessage;
import com.core.messages.MessageTypes;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Client implements Runnable{

    private EventLoopGroup workerGroup;
    private String clientType;
    private int port;
    private int id;
    private String host;

    public Client(String host, int port) throws Exception {
        this.port = port;
        this.host = host;
        switch (port) {
            case 5000:
                clientType = "Broker";
                break;
            case 5001:
                clientType = "Market";
                break;
        }
    }

    public static void inputHandler(Client client) {
        System.out.println("Handleinput");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String input;
        while (true) {
            input = null;
            try {
                input = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (input.toLowerCase().equals("exit")) {
                client.workerGroup.shutdownGracefully();
                break;
            }
        }
    }

    @Override
    public void run() {
        workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap server = new Bootstrap();
            server.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();

                            pipeline.addLast("decoder", new StringDecoder());
                            pipeline.addLast("encoder", new StringEncoder());
                            pipeline.addLast("handler", new ClientHandler());
                        }
                    });
            ChannelFuture f = server.connect(this.host, this.port).sync();
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    class ClientHandler extends ChannelInboundHandlerAdapter {
        //Handles all decoded incoming strings from server
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            System.out.println("Connected to the server - " + ctx.channel().remoteAddress());
            ctx.channel().writeAndFlush("New client added");
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println(msg);
        }

        private void channelWrite(ChannelHandlerContext ctx) {
            System.out.println("channelWrite");
            while (true) {
                try {
                    String input = getClientText();
                    if (input.equals("bye")) {
                        workerGroup.shutdownGracefully();
                    }
                    else if (clientType.equals("Broker")) {
                        ctx.writeAndFlush(input);
                        System.out.println("Sending request");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    channelWrite(ctx);
                }
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            System.out.println("channelReadComplete");
            if (clientType.equals("Broker"))
                channelWrite(ctx);
        }

        private String getClientText() throws Exception{
            System.out.println("Enter a request");
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            return in.readLine();
        }
    }

}
