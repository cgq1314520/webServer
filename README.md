# webServer
通过java实现一个简单的web服务器-包括单线程模型、多线程模型、线程池模型、业务分割模型、页面缓存模型以及文件系统等等
#### 1.com.server.SingleThread包下的代码实现的是服务器的==单线程模型==

缺点：不能并发，导致http请求到达服务端的accept（）方法部分时阻塞,只能单线程处理来自浏览器的请求，如果服务端在写一个较长的数据给浏览器时，就会导致新来的请求都阻塞在服务端的accept()处，从而导致该模型下的效率即为低下，且用户体验极差。
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

<img src="https://github.com/cgq1314520/blog-img/blob/main/image-20210121132537584.png" alt="image-20210121132537584" style="zoom:80%;" />

目的：为了解决单线程中多个http请求阻塞在accept()处等待的缺点，提高请求的并发量

- log.properties同单线程模型所述
- logGenerate.java文件为具体的日志生成的程序，同单线程中功能
- LoggerType.JAVA文件为日志类型的声明，同单线程中功能
- SingleThread.java文件为服务器多线程模型的核心处理部分，包括对于BS中浏览器请求的接收以及逻辑处理，逻辑处理的核心在service方法中

#### 3.Thread_Pool包下的代码实现时服务器的==线程池模型==
![image-20210121130835952](https://github.com/cgq1314520/blog-img/blob/main/image-20210121130835952.png)

目的：解决线程连续创建和删除的开销，建立线程池进行对任务的处理，节省连续的线程创建销毁造成的开销

- log.properties同单线程模型所述
- logGenerate.java文件为具体的日志生成的程序，同单线程中功能
- LoggerType.JAVA文件为日志类型的声明，同单线程中功能
- SingleThread.java文件为服务器的线程池模型的核心处理部分，包括对于BS中浏览器请求的接收以及逻辑处理，

#### 4.ServiceSegmentation包下的代码实现时服务器的==业务分割模型==

代码的核心逻辑即为实现了下图所示的关系

![image-20210121130800120](https://github.com/cgq1314520/blog-img/blob/main/image-20210121130800120.png)

目的：将对http请求的处理部分划分（实现类似流水线的操作），从而使得线程池中接收请求的部分可以更快的响应来自客户端的请求

由以上的图示可见，只需三个线程池以及三个任务队列即可，所以程序中实现时只需要用3个runnable和3个threadpool解决即可

- log.properties同单线程模型所述
- logGenerate.java文件为具体的日志生成的程序，同单线程中功能
- LoggerType.JAVA文件为日志类型的声明，同单线程中功能
- SingleThread.java文件为服务器业务分割模型的核心处理部分，包括对于BS中浏览器请求的接收以及逻辑处理，在业务分割模型中对于从客户端接收到的请求的处理划分为了好几个部分（也即业务分割），所以其核心的逻辑处理部分有了很大的变动，也即其中的service方法有了很大的变动，其实就是将service方法通过线程池和任务的调节，转换为有多个函数的执行，具体见代码

#### 5.pageCache包下的代码实现服务器的页面缓存模型
对于页面缓存的核心逻辑实现是通过以下的哈希表结构实现的
![image-1](https://github.com/cgq1314520/blog-img/blob/main/1.png)
也即对于页面的缓存其实就是通过一个hash表的结构进行缓存的，其中对于页面的替换算法实现了如LRU\LFU等的页面替换算法

- log.properties同单线程模型所述
- logGenerate.java文件为具体的日志生成的程序，同单线程中功能
- LoggerType.JAVA文件为日志类型的声明，同单线程中功能
- HashBucketCache.java文件为自定义的hash表结构，是页面缓存模型的核心部分，其中主要实现的功能包括页面的缓存、页面的替换条件的判定以及缓存内容的获取
- pageCache.java文件也为页面缓存模型的核心部分，主要包括对于浏览器请求的处理、根据页面的名字判断请求页是否缓存，同时通过是否缓存决定请求页内容的来源，也即是从缓存（即从内存中得到，所以非常快）中取得，还是从硬盘中取得（也即通过文件流读取，所以非常慢）

#### 6.fileSystem包下的代码实现了服务器的文件系统模型
对于文件系统模型的web浏览器，其更适合于一些页面多且小（同时不变性高）的文件，就比如说有10万个文件（就比如说淘宝页面中的10万张图片，为什么可以加载这么快，其实就相当于是一个图片服务器的感觉），且每一个文件的大小都很小，这个时
候我们就可以把这10万个网页作为一个文件系统来处理；
也即在服务器启动时，将10万个网页都写入到一个文件中，同时通过一个hash结构来记录每个网页在这个大文件中的起始位置、长度以及文件名称信息，那么当服务器完全启动之后，我们就将所有的这些小的文件都统筹到了一个大的文件中，且都加载到了内存里面，而且与此同时我们还得到了一个10万个网页在这个大文件里面的存储的位置以及长度的hash结构，那么在这个时候，如果浏览器发送了一个请求，我们就可以通过这个hash结构以及已经在内存中的文件信息将请求的文件快速的响应给浏览器，这两者的搭配将使得我们的web浏览器的响应速度大大增加。

**注意了：**

在http请求报文中比较重要的部分就是--请求行--这部分了，除此之外有时候还会用到首部行中的host主机IP属性和cookies信息，其他的部分都不太重要；由于我们需要实现的是一个静态的web服务器，所以在此我们知道请求行的这个部分就可以了

接下来先看一下http请求和响应的报文格式，以便我们书写代码时更加规范

![image-20210120113049431](https://github.com/cgq1314520/blog-img/blob/main/image-20210120113049431.png)

**请求报文的实例：**
```properties
 GET /102.html?name=cgq&password=123 HTTP/1.1     请求行
 Host: localhost:8888
 Connection: keep-alive
 Upgrade-Insecure-Requests: 1
 User-Agent: Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36
 Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9
 Sec-Fetch-Site: none
 Sec-Fetch-Mode: navigate
 Sec-Fetch-User: ?1
 Sec-Fetch-Dest: document
 Accept-Encoding: gzip, deflate, br
 Accept-Language: zh-CN,zh;q=0.9,en;q=0.8                                                                                                                                       
 这儿有个空行，其上方都是首部行，下面就是请求报文的实体部分，通常是为空
 ```
                                                                                               

**响应报文的实例：**
```properties
HTTP/1.1 200 OK      状态行                                                                                                                           
Content-Length:1222                                                                                                                                                    
Connection:close                                                                                                                                                               > 
Content-Type:image/gif;image/ico;image/jpg           
这儿有一个空行，上面的都是首部行，下面的代表的是响应的主体
响应内容的主体，比如是一个html文件的内容、或者是一个jpg文件的内容的二进制流                                                                                                     
```

**注意点：**对于流媒体的响应

在我们的这个web服务器上，对于流媒体的传输也是直接通过HTTP协议进行直接传输的，但是由于流媒体占用的内容太大，导致在单线程时只有一个流媒体可响应，直至此个响应完成之后，其他才能响应，所以在单线程中不推荐去实现流媒体的响应，但是对于多线程、线程池等web服务器模型，对于此个是支持的。
