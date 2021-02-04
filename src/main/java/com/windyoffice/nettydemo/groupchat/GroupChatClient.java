package com.windyoffice.nettydemo.groupchat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;

public class GroupChatClient {

    private final String HOST="127.0.0.1";
    private final int  PORT=6667;

    private Selector selector;

    private String userName;

    private SocketChannel socketChannel;

    public GroupChatClient(){
        try {
            selector=Selector.open();
            socketChannel=SocketChannel.open(new InetSocketAddress(HOST,PORT));
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            userName=socketChannel.getLocalAddress().toString().substring(1);
            System.out.println(userName+" is ok ......");

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void sendMsg(String info) throws IOException {
        String msg=userName+"说: "+info;
        socketChannel.write(ByteBuffer.wrap(msg.getBytes()));
    }

    public void readInfo(){
        try {
            int readChannel=selector.select();
            if(readChannel>0){
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()){
                    SelectionKey key = iterator.next();
                    if(key.isReadable()){
                        SocketChannel channel = (SocketChannel)key.channel();
                        ByteBuffer byteBuffer=ByteBuffer.allocate(1024);
                        channel.read(byteBuffer);
                        String msg=new String(byteBuffer.array());
                        System.out.println(msg.trim());
                    }
                }
                iterator.remove();
            }else{
               // System.out.println("没用可用通道");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        GroupChatClient chatClient=new GroupChatClient();
        new Thread(() -> {
            while (true){
                chatClient.readInfo();
                try {
                    Thread.sleep(3000);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();

        Scanner scanner=new Scanner(System.in);

        while (scanner.hasNextLine()){
            String s=scanner.nextLine();
            try {
                chatClient.sendMsg(s);
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }
}
