package nio;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
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

            checkMessage(message, key);
        }
    }

    private void checkMessage(String message, SelectionKey key) throws IOException {
        String[] msgParts = message.split("\\s");
        if (msgParts.length == 0)
            return;

        String command = msgParts[0];

        switch (command) {
            case SystemCommand.COMMAND_LS:
                commandLS(msgParts, key);
                break;
            case SystemCommand.COMMAND_TOUCH:
                commandTouch(msgParts, key);
                break;
            case SystemCommand.COMMAND_MKDIR:
                commandMkDir(msgParts, key);
                break;
            case SystemCommand.COMMAND_CD:
                commandCD(msgParts);
                break;
        }
    }

    private void sendMessage(String message, SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        channel.write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
    }

    private void commandLS(String[] parts, SelectionKey key) throws IOException {
        String strPath = "";
        for (int i = 1; i < parts.length; i++) {
            strPath = strPath + parts[i];
        }
        Path path = Paths.get(strPath);

        if (!Files.exists(path) || !Files.isDirectory(path)) {
            sendMessage(SystemCommand.SYS_MESSAGE_DIR_NOT_EXISTS, key);
            return;
        }

        Files.walkFileTree(path, new HashSet<>(), 1, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                sendMessage(file.toString() + System.lineSeparator(), key);
                return super.visitFile(file, attrs);
            }
        });
    }

    private void commandTouch(String[] parts, SelectionKey key) throws IOException {
        String strPath = "";
        for (int i = 1; i < parts.length; i++) {
            strPath = strPath + parts[i];
        }
        Path path = Paths.get(strPath);

        if (!Files.exists(path) || Files.isDirectory(path)) {
            sendMessage(SystemCommand.SYS_MESSAGE_DIR_NOT_EXISTS, key);
            return;
        }

        InputStream is = new FileInputStream(strPath);
        byte[] buffer = new byte[256];
        int read;

        while (true) {
            read = is.read(buffer);
            if (read == -1) {
                break;
            }

            sendMessage(new String(buffer, StandardCharsets.UTF_8) + System.lineSeparator(), key);
        }
    }

    private void commandMkDir(String[] parts, SelectionKey key) throws IOException {
        String strPath = "";
        for (int i = 1; i < parts.length; i++) {
            strPath = strPath + parts[i];
        }
        Path path = Paths.get(strPath);

        if (Files.exists(path) && Files.isDirectory(path)) {
            sendMessage(SystemCommand.SYS_MESSAGE_FILE_EXISTS, key);
            return;
        }

        Files.createDirectory(path);
    }

    private void commandCD(String[] parts) {

    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
    }
}
