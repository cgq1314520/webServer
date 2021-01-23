package com.server.ServiceSegmentation;

import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 此程序功能：其实就是实现了readme.md文件中业务分割模型图示中各个任务和线程池之间的嵌套调用关系
 * @author cgq
 * @version 2021.1.1-1
 * @apiNote  web服务器的业务分割模型中对应的几个线程池和几个的任务模型
 */
public class ThreadPools {
    //线程池1：用来存放 处理浏览器请求(主要进行对get请求中请求文件的得到) 的线程
    private static final ThreadPoolExecutor read_httpRequest_Pool;
    //线程池2：用来存放 读取http请求文件(主要读取浏览器请求的文件) 的线程
    private static final ThreadPoolExecutor read_file_Pool;
    //线程池3：用来存放 将http请求的文件写给浏览器(将线程池2中读取到的文件写给浏览器) 的线程
    private static final ThreadPoolExecutor write_httpResponse_Pool;

    //初始化
    static{
        read_httpRequest_Pool= new ThreadPoolExecutor(5,
                10,
                0,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
        read_file_Pool= new ThreadPoolExecutor(5,
                10,
                0,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
        write_httpResponse_Pool= new ThreadPoolExecutor(5,
                10,
                0,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
    }
    /**
     * 函数功能：获取用来存放 处理浏览器请求(主要进行对get请求中请求文件的得到) 的线程
     * @return 一个线程池的执行对象
     */
    protected static ThreadPoolExecutor getReadHttpRequestPool(){
        return read_httpRequest_Pool;
    }
    /**
     * 函数功能：获取用来存放 读取http请求文件(主要读取浏览器请求的文件) 的线程
     * @return 一个线程池的执行对象
     */
    protected static ThreadPoolExecutor getReadFilePool(){
        return read_file_Pool;
    }
    /**
     * 函数功能：获取用来存放 将http请求的文件写给浏览器(将线程池2中读取到的文件写给浏览器) 的线程
     * @return 一个线程池的执行对象
     */
    protected static ThreadPoolExecutor getWriteHttpResponsePool(){
        return write_httpResponse_Pool;
    }

    /**
     * 任务类1的作用：该任务类的任务为：处理浏览器请求(主要进行对get请求中请求文件的得到)这种任务
     */
    protected static class DealReadHttpRequest implements Runnable{
        private final Socket client;  //记录与浏览器的连接通道
        public DealReadHttpRequest(Socket client){
            this.client=client;
        }
        @Override
        public void run() {
            //业务分割模型中的核心逻辑部分1，也即提取浏览器请求中的文件名，并通过得到的文件名生成任务类2这种任务，用于后续业务的展开
            ServiceSegmentation.getFileName(client);
        }
    }
    /**
     * 任务类2的作用：该任务类的任务为：读取http请求的文件(主要读取浏览器请求的文件)这种任务
     */
    protected static class DealReadFile implements Runnable{
        private final Socket client;      //记录与浏览器的连接通道
        private final String fileName;    //要读取的文件名字所在全路径,由任务类1在解析时http请求时将得到的请求文件
        public DealReadFile(Socket client,String fileName){
            this.client=client;
            this.fileName=fileName;
        }
        @Override
        public void run() {
            //业务分割模型中的核心逻辑部分2，也即根据任务类1提取得到的文件名读取文件内容，并通过得到的内容生成任务类3这种任务，用于后续业务的展开
            ServiceSegmentation.getFileContentByFileName(fileName,client);
        }
    }
    /**
     * 任务类3的作用：该任务类的任务为：将http请求的响应内容(即文件内容)写给浏览器(将线程池2中读取到的文件写给浏览器)这种任务
     */
    protected static class DealWriteHttpResponse implements Runnable{
        private final Socket client;      //记录与浏览器的连接通道
        private final byte[] fileContent; //要给客户端传输的文件的内容，由任务类2的核心逻辑部分读取文件得到并存储到了字符串中
        private final int fileLength;
        public DealWriteHttpResponse(Socket client,byte []fileContent,int fileLength){
            this.client=client;
            this.fileContent=fileContent;
            this.fileLength=fileLength;
        }
        @Override
        public void run() {
            //业务分割模型中的核心逻辑部分3，也即根据任务类2读取得到的文件内容fileContent，将这个文件内容输出到对应的通道里面，用于浏览器端展示
            //最后关闭连接通道client
            ServiceSegmentation.writeFileContentToBrowser(client,fileContent,fileLength);
        }
    }
}
