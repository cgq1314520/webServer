package com.server.MultiThread;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author cgq
 * @version 2021.1.1-1
 * @apiNote web服务器的多线程模型
 */
public class MultiThread {
    public static void service(Socket client){
        FileInputStream fis=null;
        try {
            //使用Socket对象中的方法getInputStream,获取到网络字节输入流InputStream对象
            InputStream is = client.getInputStream();
            //使用网络字节输入流InputStream对象中的方法read读取客户端的请求信息
            //把is网络字节输入流对象装饰转换为字符缓冲输入流
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            //从BS连接通道中读取客户端请求信息： GET /test.html HTTP/1.1
            String line = br.readLine();
            try{
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
                    try{
                        //也许请求的文件不存在，所以在这儿，我们要进行异常捕获，当有异常时，就给浏览器返回NOTFOUND的错误
                        fis = new FileInputStream(filePath.toString());
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
                        int len;
                        byte[] bytes = new byte[2048];
                        while(-1 != (len = fis.read(bytes))) os.write(bytes, 0, len);
                        System.out.println(filePath+"文件传输成功");
                    }catch (FileNotFoundException fileNotFoundException){
                        //给浏览器返回文件NOTFOUND的错误并关闭socket通道
                        logGenerate.logger(LoggerType.NOTFOUND,filePath.append("没有找到").toString(),client);
                    }
                }
            }catch (NullPointerException e){
                logGenerate.logger(LoggerType.LOG,"请求为null",client);
            }
        }catch (IOException e){
            //  e.printStackTrace();
        }
        finally {
            //释放资源
            try {
                if(fis!=null){
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if(client!=null){
                    client.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
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
                client= server.accept();
                    if (client != null) {
                        //利用多线程模型来处理来自客户端的http请求
                        new Thread(() -> service(client)).start();
                    }

            } catch (IOException e) {
               // e.printStackTrace();
            }
        }
    }
}
