异常是java.net.SocketException: （Connection reset或者 Connect reset by peer:Socket write error）。
该异常在客户端和服务器端均有可能发生，引起该异常的原因有两个，第一个就是如果一端的Socket被关闭（或主
动关闭或者因为异常退出而 引起的关闭），另一端仍发送数据，发送的第一个数据包引发该异常 (Connect reset
 by peer)。另一个是一端退出，但退出时并未关闭该连接，另一端如果在从连接中读数据则抛出该异常（Connection
  reset）。简单的说就是在连接断开后的读和写操作引起的