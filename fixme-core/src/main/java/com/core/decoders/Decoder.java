package com.core.decoders;

import com.core.messages.AcceptConnection;
import com.core.messages.FIXMessage;
import com.core.messages.MessageTypes;
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
        if (fixMessage.getMessageType().equals(MessageTypes.ACCEPT_CONNECTION.toString())) {
            AcceptConnection ret = new AcceptConnection();
            ret.setMessageType(fixMessage.getMessageType());
            ret.setId(in.readInt());
            ret.setChecksum(in.readCharSequence(in.readInt(), cs).toString());
            out.add(ret);
        }
    }
}
