package com.server.ServiceSegmentation;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author cgq
 * @version 2021.1.1-1
 * @apiNote  web服务器的业务分割模型
 */
public class ServiceSegmentation {
    //第一步：获取用来处理浏览器请求线程的线程池
    public static ThreadPoolExecutor ReadHttpRequestPool;
    //第二步：获取用来读取文件内容线程的线程池
    public static ThreadPoolExecutor ReadFilePool;
    //第一步：获取用来将读取到的文件内容响应到客户端的线程的线程池
    public static ThreadPoolExecutor WriteHttpResponsePool;
    static{
        ReadHttpRequestPool=ThreadPools.getReadHttpRequestPool();
        ReadFilePool=ThreadPools.getReadFilePool();
        WriteHttpResponsePool = ThreadPools.getWriteHttpResponsePool();
    }

    /**
     * 具体的业务处理部分:业务分割模型的核心逻辑部分1，也即用来得到http请求文件名、并生成第2类任务的部分业务
     * @param client 连接到浏览器的通道
     */
    public static void getFileName(Socket client){
        try {
            //使用Socket对象中的方法getInputStream,获取到网络字节输入流InputStream对象
            InputStream is = client.getInputStream();
            //使用网络字节输入流InputStream对象中的方法read读取客户端的请求信息
            //把is网络字节输入流对象装饰转换为字符缓冲输入流
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            //从BS连接通道中读取客户端请求信息： GET /test.html HTTP/1.1
            String line = br.readLine();
            if(line==null){
                //如果读取BS客户端发送的消息失败，则给浏览器响应HTTP失败的消息，即FORBIDDEN
                logGenerate.logger(LoggerType.FORBIDDEN,"当前只支持简单的get请求",client);
            }
            else{

                //这儿下来则说明是请求信息发送过来了，所以记录当前请求信息为日志LOG
                logGenerate.logger(LoggerType.LOG,line,client);
                //如果已经到了这儿，则把读取的信息进行切割,只要中间部分 /test.html
                String[] arr = line.split(" ");
                //判断当前的请求是否是get请求，我们只支持get请求，其他请求返回FORBIDDEN,并关闭socket流
                if(!arr[0].toUpperCase().equals("GET")){
                    logGenerate.logger(LoggerType.FORBIDDEN,"当前只支持简单的get请求",client);
                }
                //在此判断中间的部分是否是'/'，如果是这样，则代表默认请求index.html，通过以下处理
                //定义filePath变量用来存储请求的文件路径
                StringBuilder filePath=new StringBuilder("C:\\Users\\Administrator\\Desktop\\web\\");
                if(arr[1].equals("/")){
                    filePath.append("index.html");
                }else{
                    //把路径前边的/去掉,进行截取变为test.html,并添加到待请求的路径后面
                    filePath.append(arr[1].substring(1));
                }
                //创建一个本地字节输入流,构造方法中绑定BS中浏览器请求的文件路径，用于返回
                //也许请求的文件不存在，所以在这儿，我们要进行异常捕获，当有异常时，就给浏览器返回NOTFOUND的错误
                //程序运行到这儿说明请求的文件存在，所以进行第2种任务的生成
                ThreadPools.DealReadFile dealReadFile = new ThreadPools.DealReadFile(client, filePath.toString());
                //通知第二类线程池开始任务的处理了
                ReadFilePool.execute(dealReadFile);
            }

        }catch (IOException e){
          //  e.printStackTrace();
        }
    }
    /**
     * 具体的业务处理部分:业务分割模型的核心逻辑部分2，也即用来得到http请求文件内容（放到字符串中）、并生成第3类任务的部分业务
     * @param client 连接到浏览器的通道
     * @param fileName 用来读取文件内容的全文件名，是全路径
     */
    public static void getFileContentByFileName(String fileName, Socket client){
        FileInputStream fis;
        try {
            byte[] con=new byte[Integer.MAX_VALUE/100];
            System.out.println(fileName);
            fis = new FileInputStream(fileName);
            int len;
            byte[] bytes = new byte[2048];
            int len1=0;
            while((len = fis.read(bytes))!=-1){
                System.arraycopy(bytes, 0, con, len1, len);
                len1 = len1 + len;
            }
            //文件读取完成之后进行第三类任务的生成
            ThreadPools.DealWriteHttpResponse dealWriteHttpResponse = new ThreadPools.DealWriteHttpResponse(client,con,len1);
            //通过第三类线程池进行对于任务的执行
            WriteHttpResponsePool.execute(dealWriteHttpResponse);
        } catch (IOException fileNotFoundException) {
            //文件找不到则日志输出404页面
            logGenerate.logger(LoggerType.NOTFOUND,"您所请求的网页不存在",client);
          //  fileNotFoundException.printStackTrace();
        }
    }
    public static void writeFileContentToBrowser(Socket client, byte[] fileContent,int fileLength) {
        try{
            //程序运行到这儿说明请求的文件存在，所以使用Socket中的方法getOutputStream获取网络字节输出流OutputStream对象
            OutputStream os = client.getOutputStream();
            // 写入HTTP协议响应头,响应成功，响应内容都写成text/html，注意http响应返回内容的格式，具体见readme.md内容
            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write("Content-Type:text/html\r\n".getBytes());
            // 根据http响应的格式，在此必须要写入空行,否则浏览器不解析
            os.write("\r\n".getBytes());
            //在此调用日志函数进行对于响应成功日志的记录,作为日志时，其实后面的client并没有用到
            logGenerate.logger(LoggerType.LOG,"HTTP/1.1 200 OK\r\n",client);
            //把请求的文件内容写入响应体中
            os.write(fileContent,0,fileLength);
        } catch (IOException e) {
           // e.printStackTrace();
        }
        finally {
            try {
                client.close();
            } catch (IOException e) {
              //  e.printStackTrace();
            }
        }

    }
    public static void main(String[] args) {

        //创建一个服务器ServerSocket,和系统要指定的端口号,即使用accept方法获取到请求的客户端对象(浏览器)
        ServerSocket server = null;
        try {
            server = new ServerSocket(8080);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(server!=null){
            System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"：web服务器启动成功");
        }
        // 需要注意的是我们需要知道浏览器在每遇到一个图片或者其他时都会发生一次http请求，所以说在一个网页中其实
        // 是由很多个请求构建在一起的，所以我们需要对每一次的请求都进行响应（即使是告诉浏览器，要请求的数据都不存在
        // ），否则就会阻塞在一处;
        // 也即浏览器中的图片、视频、音频等都是一个单独的http请求
        while(true){
            Socket client;
            try {
                //获取和browser连接的客户端通道
                assert server != null;
                client=server.accept();
                if(client!=null){
                    //通过这个连接通道构建任务
                    //第一步：处理接收到的http请求,形成任务
                    ThreadPools.DealReadHttpRequest dealReadHttpRequestTask=new ThreadPools.DealReadHttpRequest(client);
                    //通过第一个线程池处理第一种类型的任务
                    ReadHttpRequestPool.execute(dealReadHttpRequestTask);
                    //获取当前线程中的正在执行的线程
                    System.out.println("正在活动的线程数："+ReadHttpRequestPool.getActiveCount());
                }
            } catch (IOException e) {
             //   e.printStackTrace();
            }
        }
    }
}
