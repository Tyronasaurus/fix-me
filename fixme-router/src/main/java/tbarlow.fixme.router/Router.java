package tbarlow.fixme.router;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;

public class Router {

    private void main(String[] args) throws Exception {
        AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open();
        InetSocketAddress sAddr = new InetSocketAddress("localhost", 5000);
        server.bind(sAddr);
        System.out.format("Server is listened at %s%n", sAddr);
        MessagingAttachment attachment = new MessagingAttachment();
        attachment.server = server;
        server.accept(attachment, new ConnectionHandler());
        Thread.currentThread().join();

    }
}

class ConnectionHandler implements
        CompletionHandler<AsynchronousSocketChannel, MessagingAttachment> {
    @Override
    public void completed(AsynchronousSocketChannel client, MessagingAttachment attachment) {
        try {
            SocketAddress clientAddr = client.getRemoteAddress();
            System.out.format("Accepted a  connection from  %s%n", clientAddr);
            attachment.server.accept(attachment, this);

            ReadWriteHandler readWriteHandler = new ReadWriteHandler();
            MessagingAttachment newAttachment = new MessagingAttachment();

            newAttachment.server = attachment.server;
            newAttachment.client = client;
            newAttachment.buffer = ByteBuffer.allocate(2048);
            newAttachment.isRead = true;
            newAttachment.clientAddr = clientAddr;
            client.write(attachment.buffer, attachment, readWriteHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void failed(Throwable exc, MessagingAttachment attachment) {
        System.out.println("Failed to accept a connection.");
        exc.printStackTrace();
    }
}

class ReadWriteHandler implements CompletionHandler<Integer, MessagingAttachment> {

    @Override
    public void completed(Integer result, MessagingAttachment attachment) {
        if (result == -1) {
            try {
                attachment.client.close();
                System.out.format("Stopped listening to the client %s%n", attachment.clientAddr);
            } catch(IOException e) {
                e.printStackTrace();
            }
            return;
        }

        if (attachment.isRead) {
            attachment.buffer.flip();
            int limits = attachment.buffer.limit();
            byte bytes[] = new byte[limits];
            Charset cs = Charset.forName("UTF-8");
            String msg = new String(bytes, cs);

            System.out.format("Client at %s says: %s%n", attachment.clientAddr, msg);
            attachment.isRead = false;
            attachment.buffer.rewind();
        }
        else {
            attachment.client.write(attachment.buffer, attachment, this);
            attachment.isRead = true;
            attachment.buffer.clear();
            attachment.client.read(attachment.buffer, attachment, this);
        }
    }

    @Override
    public void failed(Throwable exc, MessagingAttachment attachment) {
        exc.printStackTrace();
    }
}

class ConnectionAttachment {
    AsynchronousServerSocketChannel server;
}
