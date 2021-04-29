package nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;

public class Server {
    private ServerSocketChannel serverChannel;
    private Selector selector;

    public Server() throws IOException {
        serverChannel = ServerSocketChannel.open();
        selector = Selector.open();
        serverChannel.bind(new InetSocketAddress(8189));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (serverChannel.isOpen()) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> selectionKeyIterator = selectionKeys.iterator();

            while (selectionKeyIterator.hasNext()) {
                SelectionKey key = selectionKeyIterator.next();
                if (key.isAcceptable())
                    handleAccept(key);
                
                if (key.isReadable())
                    handleRead(key);
                
                selectionKeyIterator.remove();
            }
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        StringBuilder stringBuilder = new StringBuilder();
        ByteBuffer buffer = ByteBuffer.allocate(256);
        int read = 0;

        while (true) {
            read = channel.read(buffer);

            if (read < 0) {
                channel.close();
                break;
            }

            if (read == 0)
                break;

            buffer.flip();
            while (buffer.hasRemaining()) {
                stringBuilder.append((char) buffer.get());
            }
            buffer.clear();

            String message = stringBuilder.toString();
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
    }
}
