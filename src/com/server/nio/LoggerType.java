package com.server.nio;

/**
 * @author cgq
 * @version 2021.1.1-1
 * @apiNote 用于服务端日志的生成选择和错误页面渲染的选择
 */
public enum LoggerType {
    //也即不支持当前的请求
    FORBIDDEN("HTTP/1.1 403 Forbidden\r\n" +
            "Connection: close\r\n" +
            "Content-Type: text/html\r\n\r\n" +
            "<html><head>\r\n" +
            "<title>403 Forbidden</title>\r\n" +
            "</head><body>\r\n" +
            "<h1>",
            "Forbidden",
            "</h1>\r\n " +
                    "The requested URL, file type or operation is not allowed on this simple static file webserver.\r\n" +
                    "</body></html>\r\n"),
    NOTFOUND("HTTP/1.1 404 Not Found\r\n" +
            "Connection: close\r\n" +
            "Content-Type: text/html\r\n\r\n" +
            "<html><head>\r\n" +
            "<title>404 Not Found</title>\r\n" +
            "</head><body>\r\n" +
            "<h1>",
            "Not found",
            "</h1>\r\n " +
                    "The requested URL, file was not found on this server.\r\n" +
                    "</body></html>\r\n"),
    ERROR_REQUEST("HTTP/1.1 400 Not Found\r\n" +
            "Connection: close\r\n" +
            "Content-Type: text/html\r\n\r\n" +
            "<html><head>\r\n" +
            "<title>400 Not Found</title>\r\n" +
            "</head><body>\r\n" +
            "<h1>",
            "Not found",
            "</h1>\r\n " +
                    "The requested URL, file was not found on this server.\r\n" +
                    "</body></html>\r\n"),
    SYSTEM_EXCEPTION("HTTP/1.1 500 System Exception\r\n" +
            "Connection: close\r\n" +
            "Content-Type: text/html\r\n\r\n" +
            "<html><head>\r\n" +
            "<title>500 System exeption</title>\r\n" +
            "</head><body>\r\n" +
            "<h1>",
            "System exception",
            "</h1>\r\n " +
                    "System exception\r\n" +
                    "</body></html>\r\n"),
    LOG("LOG", "", "");
    private final String header;
    private String content;
    private String end;

    LoggerType(String header, String content, String end) {
        this.header = header;
        this.content = content;
        this.end = end;
    }

    protected String getHeader() {
        return header;
    }

    protected String getContent() {
        return content;
    }

    protected String getEnd() {
        return end;
    }
}
