package com.server.nio;

/**
 * @author Administrator
 * @Description
 * @create 2023-01-16 19:20
 */


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * TinyHttpd
 *
 * @author code4wt
 * @date 2018-03-26 22:28:44
 */
class Headers {
    private String method;
    private String path;
    private String proto;
    private HashMap<String, String> keyMap = new HashMap<>();

    Headers() {
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getProto() {
        return proto;
    }

    public void setProto(String proto) {
        this.proto = proto;
    }

    public void set(String key, String value) {
        keyMap.put(key, value);
    }

    public String get(String key) {
        return keyMap.get(key);
    }

    @Override
    public String toString() {
        return "Headers{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", proto='" + proto + '\'' +
                ", keyMap=" + keyMap +
                '}';
    }
}

public class Nio {

    private static final int DEFAULT_PORT = 8080;
    private static final int DEFAULT_BUFFER_SIZE = 4096;
    private static final String WELCOME_PAGE = "index.html";
    private static final String DEFAULT_STATIC_DIR = logGenerate.getProperties()==null? "./":(logGenerate.getProperties().getProperty("web.static.file.path")==null?"./":logGenerate.getProperties().getProperty("web.static.file.path"));
    private static final String KEY_VALUE_SEPARATOR = ":";
    private static final String CRLF = "\r\n";
    private static boolean KEEP_ALIVE = false;
    private static final String CONNECT_INFO_KEY = "Connection";
    private static final String CONNECT_INFO_VALUE = "keep-alive";

    private int port;

    public Nio() {
        this(DEFAULT_PORT);
    }

    public Nio(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"：web服务器启动成功");
        //1. 初始化 ServerSocketChannel
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress("localhost", port));
        serverSocketChannel.configureBlocking(false);

        //2. 创建 Selector
        Selector selector = Selector.open();

        //3. 在select中注册 接受 事件,用于监听accept成功的通道信息
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        //4. 开始循环处理
        while (true) {
            int num = selector.select();
            if (num == 0) {
                continue;
            }
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> it = selectedKeys.iterator();
            while (it.hasNext()) {
                SelectionKey selectionKey = it.next();
                it.remove();
                //5. 开始处理  SelectionKey.OP_READ SelectionKey.OP_WRITE SelectionKey.OP_ACCEPT各个事件
                if (selectionKey.isAcceptable()) {
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    socketChannel.configureBlocking(false);
                    //todo 注册感兴趣的事件为可读,当有可读的数据到达后就会使得isReadable=true
                    socketChannel.register(selector, SelectionKey.OP_READ);
                } else if (selectionKey.isReadable()) {
                    // 处理请求
                    request(selectionKey);
                    //todo 注册感兴趣的事件为可写,当缓冲区可写时,isWritable为true,所以每次响应写完数据后,需要将写事件从interestOps中移除
                    selectionKey.interestOps(SelectionKey.OP_WRITE);
                } else if (selectionKey.isWritable()) {
                    // 响应请求
                    response(selectionKey);
                }
            }
        }
    }

    private void request(SelectionKey selectionKey) throws IOException {
        // 从通道中读取请求头数据
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
        channel.read(buffer);
        //切换buffer为读模式
        buffer.flip();
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        String headerStr = new String(bytes);
        try {
            // 解析请求头
            Headers headers = parseHeader(headerStr);
            // 将请求头对象放入 selectionKey 中
            selectionKey.attach(Optional.of(headers));
        } catch (Exception e) {
            selectionKey.attach(Optional.empty());
        }
    }

    private Headers parseHeader(String headerStr) throws Exception {
        if (Objects.isNull(headerStr) || headerStr.isEmpty()) {
            throw new Exception("http1.1 protocol is not right,head String is null");
        }

        // 解析请求头第一行,获取请求路径
        int index = headerStr.indexOf(CRLF);
        if (index == -1) {
            throw new Exception("http1.1 protocol is not right,crlf is not right");
        }

        Headers headers = new Headers();
        String firstLine = headerStr.substring(0, index);
        String[] parts = firstLine.split(" ");
        if (parts.length < 3) {
            throw new Exception("http1.1 protocol is not right,first line is not right");
        }

        headers.setMethod(parts[0]);
        headers.setPath(parts[1]);
        headers.setProto(parts[2]);

        // 解析请求头部分内容
        parts = headerStr.split(CRLF);
        for (String part : parts) {
            index = part.indexOf(KEY_VALUE_SEPARATOR);
            if (index == -1) {
                continue;
            }
            String key = part.substring(0, index);
            if (index == -1 || index + 1 >= part.length()) {
                headers.set(key, "");
                continue;
            }
            String value = part.substring(index + 1);
            headers.set(key, value);
            if(key.equals(CONNECT_INFO_KEY)){
                if(value.contains(CONNECT_INFO_VALUE)){
                    KEEP_ALIVE = true;
                }
            }
        }

        return headers;
    }

    private void response(SelectionKey selectionKey) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        // 从 selectionKey 中取出请求头对象
        Optional<Headers> op = (Optional<Headers>) selectionKey.attachment();

        // 处理无效请求，返回 400 错误
        if (!op.isPresent()) {
            handleErrorRequest(channel);
            return;
        }
        Headers headers = op.get();
        try {
            //响应正常的网页内容
            handleSuccess(channel, headers.getPath());
            selectionKey.interestOps(selectionKey.interestOps() - SelectionKey.OP_WRITE);
        } catch (FileNotFoundException e) {
            // 文件未发现，返回 404 错误
            handleNotFound(channel);
        } catch (Exception e) {
            // 其他异常，返回 500 错误
            handSystemException(channel,e.getMessage());
        } finally {
            //完成一次写后，去除写
            if(!KEEP_ALIVE){
                selectionKey.channel().close();
            }
        }
    }

    // 处理正常的请求
    private void handleSuccess(SocketChannel channel, String path) throws IOException {

        // 读取文件
        ByteBuffer bodyBuffer = readFile(path);
        // 设置响应头
        String headers = "HTTP/1.1 200 OK\r\n" + "Content-Type:text/html;charset=utf-8\r\n" + "Connection:keep-alive\r\n" +"Content-Length:"+bodyBuffer.capacity()+"\r\n"+ "\r\n";
        ByteBuffer headBuffer = ByteBuffer.allocate(headers.getBytes().length);
        headBuffer.put(headers.getBytes());
        //将position变为0，后续写操作复制数据时可以从0位置开始复制，进而复制得到值
        bodyBuffer.flip();
        headBuffer.flip();
        // 将响应头和资源数据一同返回
        long write = channel.write(new ByteBuffer[]{headBuffer, bodyBuffer});
    }

    // 404错误
    private void handleNotFound(SocketChannel channel) {
        logGenerate.logger(LoggerType.NOTFOUND,"fileNotFound",channel);
    }

    /**
     * 系统解析异常错误
     * @param channel
     * @param errorMessage
     */
    private void handSystemException(SocketChannel channel,String errorMessage) {
        logGenerate.logger(LoggerType.SYSTEM_EXCEPTION,"fileNotFound",channel);
    }

    /**
     * 错误请求处理
     * @param channel
     */
    private void handleErrorRequest(SocketChannel channel) {
        logGenerate.logger(LoggerType.ERROR_REQUEST,"bad request",channel);
    }




    private ByteBuffer readFile(String path) throws IOException {
        if (path.equals("/")) {
            path = DEFAULT_STATIC_DIR + File.separator + WELCOME_PAGE;
        } else{
            path = DEFAULT_STATIC_DIR + path;
        }
        System.out.println("filePath:"+path);
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException();
        }
        FileInputStream responseInfo = new FileInputStream(file);
        FileChannel fileChannel = responseInfo.getChannel();
        long fileSize = fileChannel.size();
        //fileSize不能为是超过5m的数据，则说明一定不是一个long类型，可以强转为int类型
        ByteBuffer detailByte = ByteBuffer.allocate((int) fileSize);
        int read = fileChannel.read(detailByte);
        return detailByte;
    }
    public static void main(String[] args) throws IOException {
        Nio nioHttpServer = new Nio();
        nioHttpServer.start();
    }
}