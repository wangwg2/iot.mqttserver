package mqtt;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mqtt.handle.ConnectionService;
import mqtt.handle.PushServiceHandle;
import mqtt.handle.SubServiceHandle;


/**
 * 
 * @author tzj
 *
 */
public class MqttServer {
	private static final int port = 8964;
	
	protected static final int BIZGROUPSIZE = Runtime.getRuntime().availableProcessors()*2; //
	

	ConcurrentHashMap<String, Channel> str2channel=new ConcurrentHashMap<String, Channel>();

	ConcurrentHashMap<Channel, String> channel2str=new ConcurrentHashMap<Channel, String>();

	ConcurrentHashMap<String, List<String>> submap=new ConcurrentHashMap<String, List<String>>();
	
	ConcurrentHashMap<String, Integer> topContent =new ConcurrentHashMap<String, Integer>();
	 
	 final ExecutorService executorService=Executors.newFixedThreadPool(1);
	
	
	

	public void start() throws InterruptedException {
		
		//executorService=Executors.newFixedThreadPool(10);
		ServerBootstrap bootstrap=new ServerBootstrap();//引导辅助程序
      
		// 通过nio方式来接收连接和处理连// 通过nio方式来接收连接和处理连接
		
		NioEventLoopGroup group=new NioEventLoopGroup(4);
				
		final NioEventLoopGroup workGroup=new NioEventLoopGroup(8);
	
		final NioEventLoopGroup dboptGroup=new NioEventLoopGroup(8);
		
		
		bootstrap.group(group,workGroup);
	
		bootstrap.channel(NioServerSocketChannel.class);// 设置nio类型的channel
		bootstrap.localAddress(new InetSocketAddress(port));// 设置监听端口
		bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
		try {
			bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {// 有连接到达时会创建一个channel
				protected void initChannel(SocketChannel ch) throws Exception {
					
					
                    ch.pipeline()
                   .addLast(new ReadTimeoutHandler(50))
                   .addLast(MqttEncoder.INSTANCE)
                    .addLast(new MqttDecoder())
                    .addLast(new ConnectionService(dboptGroup))
                    .addLast(new SubServiceHandle())
                    .addLast(new PushServiceHandle(dboptGroup));
					
				}
				
				
			});
			ChannelFuture f = bootstrap.bind().sync();// 配置完成，开始绑定server，通过调用sync同步方法阻塞直到绑定成功
			System.out.println(MqttServer.class.getName()
					+ " started and listen on " + f.channel().localAddress());
	     		
			f.channel().closeFuture().sync();// 应用程序会一直等待，直到channel关闭
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			group.shutdownGracefully().sync();// 关闭EventLoopGroup，释放掉所有资源包括创建的线程
			workGroup.shutdownGracefully().sync();
		}
	}

	
}