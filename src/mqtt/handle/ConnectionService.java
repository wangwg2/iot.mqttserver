package mqtt.handle;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.session.SqlSession;

import mqtt.db.DBSessionFactory;
import mqtt.entity.UserExample;
import mqtt.service.ChannelDataService;

/**
 * 
 * @author 处理连接andle
 *
 */
public class ConnectionService extends ChannelInboundHandlerAdapter {

	
	/**
	 * 
	 */
	ConcurrentHashMap<String, Channel> str2channel;

	ConcurrentHashMap<Channel, String> channel2str;
	
	NioEventLoopGroup dboptgroup;
	
	 final  String exisUserPass = "mqtt.entity.UserMapper.countByExample";//映射sql的标识字符串
	
	
	public ConnectionService(NioEventLoopGroup dboptgroup) {
		super();
		this.str2channel = ChannelDataService.getStr2channel();
		this.channel2str = ChannelDataService.getChannel2str();
		this.dboptgroup=dboptgroup;
	}


	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
	}

	
	public void channelReadComplete(ChannelHandlerContext ctx) {
		try {
			super.channelReadComplete(ctx);
		} catch (Exception e) {
			e.printStackTrace();
		}
		ctx.flush();

	}

	@Override
	public void channelInactive(final ChannelHandlerContext ctx)
			throws Exception {
		super.channelInactive(ctx);

		loginout(ctx.channel());
		ctx.close();
	}
	
	public void channelRead(ChannelHandlerContext ctx, Object msg) {

		if (msg instanceof MqttMessage) {

			MqttMessage message = (MqttMessage) msg;
			MqttMessageType messageType = message.fixedHeader().messageType();

			switch (messageType) {
			case CONNECT:

				MqttConnectMessage connectMessage = (MqttConnectMessage) message;
                
				ack(ctx, connectMessage);
				break;
			case PINGREQ:
				ping(ctx);
				break;

			case DISCONNECT:
				loginout(ctx.channel());
				break;
			default:
				ctx.fireChannelRead(msg);
				break;
			}

		}

		else
			ctx.channel().close();
	}
	
	/**
	 * l锟斤拷
	 * 
	 * @param ctx
	 * @param connectMessage
	 */
	private void ack(final ChannelHandlerContext ctx,
			final MqttConnectMessage connectMessage) {

		
		dboptgroup.submit(new  Runnable() {
			
			@Override
			public void run() {
				
				
				
				MqttConnectPayload connectPayload=connectMessage.payload();
				 UserExample example=new UserExample();
				 
				 example.createCriteria().
				 andUsernameEqualTo(connectPayload.userName()).andPasswordEqualTo(connectPayload.password());
				 SqlSession session= DBSessionFactory.getSqlSession();
				 Integer integer=session.selectOne(exisUserPass,example);
				 session.close();
				
				String ident = connectPayload.clientIdentifier();
				
				MqttFixedHeader fixedHeader = new MqttFixedHeader(
						MqttMessageType.CONNACK, false, MqttQoS.AT_LEAST_ONCE, false,0);
				MqttConnAckVariableHeader connectVariableHeader = null;
				
                if(integer<=0){
                	connectVariableHeader= new MqttConnAckVariableHeader(
							MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, false);
                }
                else if(str2channel.containsKey(ident)){		
					connectVariableHeader= new MqttConnAckVariableHeader(
							MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, false);
				}else{
					connectVariableHeader= new MqttConnAckVariableHeader(
							MqttConnectReturnCode.CONNECTION_ACCEPTED, false);
					System.out.println(ident + "连接数量"+str2channel.size());
					
					str2channel.put(ident, ctx.channel());
					channel2str.put(ctx.channel(), ident);
				}
				
				

				MqttConnAckMessage ackMessage = new MqttConnAckMessage(fixedHeader,
						connectVariableHeader);
				ctx.writeAndFlush(ackMessage);
				
			}
		});
	 
	
		

	}

	
	/**
	 * 锟斤拷锟斤拷
	 * @param ctx
	 */
	private void ping(ChannelHandlerContext ctx) {

		MqttFixedHeader fixedHeader = new MqttFixedHeader(
				MqttMessageType.PINGRESP, false, MqttQoS.AT_LEAST_ONCE, false,
				0);

		MqttMessage mqttMessage = new MqttMessage(fixedHeader);
		ctx.channel().writeAndFlush(mqttMessage);
	}
	
	
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();// 锟斤拷捉锟届常锟斤拷息
		
		loginout(ctx.channel());
		

	}
	void  loginout(Channel channel){
		
		String iden = channel2str.get(channel);

		if(iden!=null){
		str2channel.remove(iden);
		System.out.println(iden + "锟剿筹拷锟斤拷" + str2channel.size());

		channel2str.remove(channel);
		System.out.println(channel + "锟较匡拷锟斤拷" + channel2str.size());
		}
		
		channel.close();
	}
}
