# webServer
通过java实现一个简单的web服务器-包括单线程模型、多线程模型、线程池模型、业务分割模型、页面缓存模型以及文件系统等等
#### 1.com.server.SingleThread包下的代码实现的是服务器的==单线程模型==

缺点：不能并发，导致服务端程序运行在accept（）部分时阻塞

- log.properties是是否生成日志及日志格式的配置文件，其基本内容为

  ```properties
  #用log.size 来配置文件的默认大小，单位默认为K -注意：注释前面不要带空格
  log.size=5
  #用log.control 来配置日志的开关，open代表开，close代表不生成日志
  log.control=open
  #用log.location来配置日志文件的生成位置，如果不配置，则默认生成到以下目录
  log.location=C:\\Users\\Administrator\\Desktop\\web\\
  #用log.dateFormat来配置生成日志时前面附带的时间格式，默认即为前面的这种形式
  log.dateFormat=yyyy-MM-dd HH:mm:ss
  ```

- logGenerate.java文件为具体的日志生成的程序，它可以通过以上文件中给定的配置进行日志内容的输出

- LoggerType.JAVA文件为日志类型的声明，也即对当前到底是纯粹的日志的输出，还是错误页面类型的输出

- SingleThread.java文件为服务器单线程模型的核心处理部分，包括对于BS中浏览器请求的接收以及逻辑处理，逻辑处理的核心在service方法中

#### 2.com.server.MultiThread包下的代码实现的是服务器的==多线程模型==
<img src="C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\image-20210121132537584.png" alt="image-20210121132537584" style="zoom:80%;" />

目的：为了解决单线程中多个http请求阻塞在accept()处等待的缺点，提高请求的并发量

- log.properties同单线程模型所述
- logGenerate.java文件为具体的日志生成的程序，同单线程中功能
- LoggerType.JAVA文件为日志类型的声明，同单线程中功能
- SingleThread.java文件为服务器多线程模型的核心处理部分，包括对于BS中浏览器请求的接收以及逻辑处理，逻辑处理的核心在service方法中

#### 3.Thread_Pool包下的代码实现时服务器的==线程池模型==
![image-1](https://github.com/cgq1314520/blog-img/blob/main/banner2.jpg)
![image-20210121130835952](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\image-20210121130835952.png)

目的：解决线程连续创建和删除的开销，建立线程池进行对任务的处理，节省连续的线程创建销毁造成的开销

- log.properties同单线程模型所述
- logGenerate.java文件为具体的日志生成的程序，同单线程中功能
- LoggerType.JAVA文件为日志类型的声明，同单线程中功能
- SingleThread.java文件为服务器的线程池模型的核心处理部分，包括对于BS中浏览器请求的接收以及逻辑处理，

#### 4.ServiceSegmentation包下的代码实现时服务器的==业务分割模型==

代码的核心逻辑即为实现了下图所示的关系

![image-20210121130800120](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\image-20210121130800120.png)

目的：将对http请求的处理部分划分（实现类似流水线的操作），从而使得线程池中接收请求的部分可以更快的响应来自客户端的请求

由以上的图示可见，只需三个线程池以及三个任务队列即可，所以程序中实现时只需要用3个runnable和3个threadpool解决即可

- log.properties同单线程模型所述
- logGenerate.java文件为具体的日志生成的程序，同单线程中功能
- LoggerType.JAVA文件为日志类型的声明，同单线程中功能
- SingleThread.java文件为服务器业务分割模型的核心处理部分，包括对于BS中浏览器请求的接收以及逻辑处理，在业务分割模型中对于从客户端接收到的请求的处理划分为了好几个部分（也即业务分割），所以其核心的逻辑处理部分有了很大的变动，也即其中的service方法有了很大的变动，其实就是将service方法通过线程池和任务的调节，转换为有多个函数的执行，具体见代码

**注意了：**

在http请求报文中最重要的部分就是--请求行--这部分了，除此之外有时候还会用到首部行中的host主机IP属性和cookies信息，其他的部分都不太重要；由于我们需要实现的是一个静态的web服务器，所以在此我们知道请求行的这个部分就可以了



接下来请牢记http请求和响应的报文格式;

![image-20210120113049431](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\image-20210120113049431.png)

**请求报文的实例：**

> GET /102.html?name=cgq&password=123 HTTP/1.1     请求行
> Host: localhost:8888
> Connection: keep-alive
> Upgrade-Insecure-Requests: 1
> User-Agent: Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36
> Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9
> Sec-Fetch-Site: none
> Sec-Fetch-Mode: navigate
> Sec-Fetch-User: ?1
> Sec-Fetch-Dest: document
> Accept-Encoding: gzip, deflate, br
> Accept-Language: zh-CN,zh;q=0.9,en;q=0.8
> Cookie: UM_distinctid=174b40effd4281-086a8ed1aac0b3-376b4502-1fa400-174b40effd5261; CNZZDATA155540=cnzz_eid%3D516937032-1600747108-%26ntime%3D1600747108; __utmz=111872281.1603867462.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); __utma=111872281.423618548.1603867462.1603884164.1603957706.3; Idea-8296e770=dacd4b86-eb35-4597-9b48-1c0d6bae2cc9; theme=#EE10EA                                                                                                                                                                                这儿有个空行，其上方都是首部行，下面就是请求报文的实体部分，通常是为空
>
> ?                                                                                                

**响应报文的实例：**

> HTTP/1.1 200 OK                                状态行                                                                                                                                                      Content-Length:1222                                                                                                                                                                           Connection:close                                                                                                                                                                                            Content-Type:image/gif;image/ico;image/jpg           
>
> 这儿有一个空行，    上面的都是首部行，下面的代表的是响应的主体
>
> 响应内容的主体，比如是一个html文件的内容、或者是一个jpg文件的内容的二进制流                                                                                                                                                     





**注意点：**对于流媒体的响应

对于流媒体的响应也是通过HTTP协议进行直接传输的，也即如视频、音频等的传输，其实就是http请求的$*/*$这种请求中包含的内容，但是我们又知道一般所有的流媒体，他们占用的内存都比较大，所以其实是在我们点击时才开始响应的，==同时服务端对于流媒体的数据流响应，即使我们用的是while循环一只写，但是在这个里面其实当写一部分流媒体内容后就会停止了，以保证响应的快速==，其实就是下面的这种效果

![image-20210120170436982](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\image-20210120170436982.png)

也即上面的四个流媒体，当我们点击其中一个音频或者视频时，我们必须要等到该视频播放完后，点击另外一首才能开始播放，否则是没有效果的，造成这个的主要原因是由于我们的服务端是单线程的，由于之前我们说的，对于流媒体在while循环里面即使是一直在写出，但是由于为了快速响应，所以在写出一段后就会等待，等到播放到这儿后才会继续进行传输，所以这也是为什么当当前的一首播放时，另外一首不能播放的原因，因为当前是单线程，所以其实是阻塞在while循环里面，等待着给客户端输出流的，所以等到一首播放完之后，才能回到while（true）的部分继续接受响应。具体的流传输如下代码所示：

![image-20210120171357113](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\image-20210120171357113.png)

所以在上面的这种单线程模型下，如果一首歌没有播放完，那么点击其他的播放按钮就不会响应，这是因为当前的一个音频还阻塞在while循环这儿，所以没有响应，同时这个也是为什么在响应html之后，输出中会抛出异常的原因；

单线程请求时异常为：

![image-20210120172521760](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\image-20210120172521760.png)

![image-20210120172551323](C:\Users\Administrator\AppData\Roaming\Typora\typora-user-images\image-20210120172551323.png)

?       单线程模型代码如下：

```java
package com.server.singleThread;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
/**
 * @author cgq
 * @version 2021.1.1-1
 * @deprecated web服务器的单线程模型
 */
public class SingleThread1 {
    public static void web(ServerSocket server){
        // new Thread(new Runnable() {
        //    @Override
        //   public void run() {
        Socket socket=null;
        FileInputStream fis=null;
        try {
            //使用accept方法获取到请求的客户端对象(浏览器)
           socket = server.accept();
            //使用Socket对象中的方法getInputStream,获取到网络字节输入流InputStream对象
            InputStream is = socket.getInputStream();
            //使用网络字节输入流InputStream对象中的方法read读取客户端的请求信息

            //把is网络字节输入流对象,转换为字符缓冲输入流
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            //把客户端请求信息的第一行读取出来 GET /11_Net/web/index.html HTTP/1.1
            String line = br.readLine();
            System.out.println(line);
            //把读取的信息进行切割,只要中间部分 /11_Net/web/index.html
            String[] arr = line.split(" ");
            //把路径前边的/去掉,进行截取 11_Net/web/index.html
            String htmlpath = arr[1].substring(1);

            //创建一个本地字节输入流,构造方法中绑定要读取的html路径
            fis = new FileInputStream("C:\\Users\\Administrator\\Desktop\\web\\"+htmlpath);
            //使用Socket中的方法getOutputStream获取网络字节输出流OutputStream对象
            OutputStream os = socket.getOutputStream();

            // 写入HTTP协议响应头,固定写法
            os.write("HTTP/1.1 200 OK\r\n".getBytes());
            os.write("Content-Type:text/html\r\n".getBytes());
            // 根据http响应的格式，在此必须要写入空行,否则浏览器不解析
            os.write("\r\n".getBytes());

            //一读一写复制文件,把服务读取的html文件回写到客户端，也即写入响应体
            int len = 0;
            byte[] bytes = new byte[2048];
            while((len = fis.read(bytes))!=-1){
                System.out.println("开始传输"+htmlpath+"文件，长度为"+len);
                os.write(bytes,0,len);
            }
            System.out.println(htmlpath+"文件传输成功");


        }catch (IOException e){
            e.printStackTrace();
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
                if(socket!=null){
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args) {
        //创建一个服务器ServerSocket,和系统要指定的端口号
        ServerSocket server = null;
        try {
            server = new ServerSocket(8080);
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*
            浏览器解析服务器回写的html页面,页面中如果有图片,那么浏览器就会单独的开启一个线程,读取服务器的图片
            我们就的让服务器一直处于监听状态,客户端请求一次,服务器就回写一次
         */
        for (int i=0;;i++){
            if(server!=null){
                web(server);
            }
        }
          //  }).start();
       // }
    }

}

```

?          

###      所以推出结论：在单线程中，不要用流媒体，要用就用一个流媒体就好了，如果想要用多个流媒体，就用多线程模型
