package com.windyoffice.nettydemo.groupchat;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

/***
 * 群聊系统的服务端
 */
public class GroupChatServer {
    // 定义属性
    private Selector selector;

    private ServerSocketChannel listenChannel;

    private static final  int port =6667;

    // 构造器
    //初始化工作

    public  GroupChatServer(){
        try {
            // 拿到选择器
            selector=Selector.open();
            //初始 ServerSocketChannel
            listenChannel=ServerSocketChannel.open();
            //绑定端口
            listenChannel.socket().bind(new InetSocketAddress(port));
            //设置非阻塞模式
            listenChannel.configureBlocking(false);
            //将listenChannel注册到selector上
            listenChannel.register(selector, SelectionKey.OP_ACCEPT);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public  void listen(){
        try {
            //循环处理
            while (true){
                int count = selector.select(2000);
                if(count>0){
                    //有相关Channel通道需要处理
                    Iterator<SelectionKey> selectionKeyIterator = selector.selectedKeys().iterator();
                    while (selectionKeyIterator.hasNext()){
                        SelectionKey key = selectionKeyIterator.next();
                        //监听到accept
                        if(key.isAcceptable()){
                            SocketChannel sc = listenChannel.accept();
                            sc.configureBlocking(false);
                            sc.register(selector,SelectionKey.OP_READ);
                            System.out.println(sc.getRemoteAddress()+"上线了");
                        }
                        if(key.isReadable()){
                            //通道有可读事件发生
                            read(key);
                        }
                        //移除当前key
                        selectionKeyIterator.remove();
                    }
                }else{
                    System.out.println("等待连接中.......");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {

        }
    }

    //读取客户端消息
    private void read(SelectionKey selectionKey){
        //定义一个SocketChannel
        SocketChannel socketChannel=null;
        try {
            socketChannel=(SocketChannel) selectionKey.channel();
            //创建缓冲buffer
            ByteBuffer byteBuffer=ByteBuffer.allocate(1024);
            int count=socketChannel.read(byteBuffer);
            if(count>0){
                //把缓冲区的数据字符串输出
                String msg=new String(byteBuffer.array());
                //输出消息
                System.out.println("服务器收到来自客户端的消息:"+msg);
                //向其它客户端转发消息
                sendInfoToOtherClients(msg,socketChannel);
            }
        }catch (IOException e){
            try {
                System.out.println(socketChannel.getRemoteAddress()+" 离线 ");
                String msg2=socketChannel.getRemoteAddress()+" 离线了 ";
                sendInfoToOtherClients(msg2,socketChannel);
                //取消注册
                selectionKey.cancel();
                //关闭通道
                socketChannel.close();
            }catch (IOException e2){
                e2.printStackTrace();
            }

        }

    }

    //转发消息给其他客户
    private void sendInfoToOtherClients(String msg,SocketChannel self) throws IOException {
        System.out.println("服务器转发消息开始 ......");
        //遍历所有注册的selector上的socketChannel
        for(SelectionKey key : selector.keys()){
            Channel targetChannel = key.channel();
            // 排除自已
            if(targetChannel instanceof  SocketChannel && targetChannel!=self){
                SocketChannel destSocketChannel=(SocketChannel)targetChannel;
                ByteBuffer byteBuffer = ByteBuffer.wrap(msg.getBytes());
                destSocketChannel.write(byteBuffer);
            }

        }
    }

    public static void main(String[] args) {
        GroupChatServer server=new GroupChatServer();
        server.listen();
    }
}
