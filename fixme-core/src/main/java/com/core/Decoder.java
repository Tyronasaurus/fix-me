package com.core;

import com.core.messages.FIXMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.nio.charset.Charset;
import java.util.List;

public class Decoder extends ReplayingDecoder<Object> {
    //Decodes bytes into strings
    private Charset cs = Charset.forName("UTF-8");
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        FIXMessage fixMessage = new FIXMessage();
        fixMessage.setMessageType(in.readCharSequence(in.readInt(), cs).toString());
        out.add(fixMessage);
    }
}
