package mqtt.handle;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.ibatis.session.SqlSession;

import mqtt.db.DBSessionFactory;
import mqtt.entity.MsgRep;
import mqtt.entity.MsgRepExample;
import mqtt.entity.TransportMessage;
import mqtt.service.ChannelDataService;
import mqtt.service.MessageDataService;

public class PushServiceHandle extends ChannelInboundHandlerAdapter {

	
	final  String saveMsg="mqtt.entity.MsgRepMapper.insert";
	
	final  String queryMsg="mqtt.entity.MsgRepMapper.selectByExample";
	
	ConcurrentHashMap<String, Channel> str2channel;
	
	ConcurrentHashMap<Channel, String> channel2str;

	/**
	 * 每一个客户端订阅的主题
	 */
	ConcurrentHashMap<String, List<String>> submap;
	
	/**
	 * 主题消息 不清除
	 */
	ConcurrentHashMap<String, Integer> topContent;
	/**
	 * 所有消息  不清除
	 */
	ConcurrentHashMap<Integer, TransportMessage> messages;
	
	/**
	 * 已经发送过的消息  不能清除
	 */
	ConcurrentHashMap<String, ConcurrentLinkedQueue<Integer>> messageSends;
	
	NioEventLoopGroup dboptgroup;
	
	
	
	public PushServiceHandle(NioEventLoopGroup dboptgroup){
		
		messages=MessageDataService.getMessages();
		messageSends=MessageDataService.getMessageSends();
		topContent=MessageDataService.getTopContent();
		
		str2channel=ChannelDataService.getStr2channel();
		channel2str=ChannelDataService.getChannel2str();
		
		this.dboptgroup=dboptgroup;
	
		
	}
	
	
	public void channelRead(ChannelHandlerContext ctx, Object msg) {

		if (msg instanceof MqttMessage) {

			MqttMessage message = (MqttMessage) msg;
			MqttMessageType messageType = message.fixedHeader().messageType();

			switch (messageType) {
			
			case PUBLISH://客户端发布普通消息
				MqttPublishMessage messagepub = (MqttPublishMessage) msg;
				pub(ctx, messagepub);
				break;
				
			case  PUBREL: //客户端发布释放
				pubrel(ctx, message);
				break;
			case PUBREC://客户端发布收到
				pubrec(ctx, message);
			default:
				ctx.fireChannelRead(msg);
				break;
			}

		}

		else
			ctx.channel().close();
	}
	
	/**
	 * 发布消息
	 * 
	 * 根据客户端发来的QOS级别 返回相应的 pub回复包，并且 设置相应的QOS级别
	 * 取出数据包里面的数据  存储
	 * @param ctx
	 * @param messagepub
	 */
	private void pub(final ChannelHandlerContext ctx,
			MqttPublishMessage messagepub) {
		
		final int  messageid=messagepub.variableHeader().messageId();
		
		
		MqttQoS mqttQoS=messagepub.fixedHeader().qosLevel();
		
		MqttFixedHeader fixedHeader=null;
		boolean  islastone=false; 
		if(mqttQoS.value()<=1){
			//不是级别最高的QOS  返回 puback 即可
			fixedHeader	= new MqttFixedHeader(
					MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
		}
		else{
			//否则发送发布收到  QOS级别会2
			fixedHeader	= new MqttFixedHeader(
					MqttMessageType.PUBREC, false, MqttQoS.EXACTLY_ONCE, false, 0);
		}
		
		

		MqttMessageIdVariableHeader connectVariableHeader = MqttMessageIdVariableHeader
				.from(messageid);

		MqttPubAckMessage ackMessage = new MqttPubAckMessage(fixedHeader,
				connectVariableHeader);
		ctx.writeAndFlush(ackMessage);
		
		
		
		final	String topname = messagepub.variableHeader().topicName();

		 ByteBuf buf = messagepub.content();
		final byte[] bs = new byte[buf.readableBytes()];
		
		
		
		TransportMessage message=new TransportMessage();
		message.setContent(bs);
		message.setMessageId(messageid);
		message.setTopName(topname);
		buf.readBytes(bs);
		
		
		dboptgroup.submit(new Runnable() {
			
			@Override
			public void run() {
				
				SqlSession session= DBSessionFactory.getSqlSession();
				MsgRep msgRep=new MsgRep();
				msgRep.setContent(bs);
				msgRep.setTopname(topname);
				msgRep.setMessageid(messageid);
				session.insert(saveMsg,msgRep);
				session.commit();
				
			}
		});
		messages.put(messageid, message);
		
		
	   if(islastone) {//如果只是普通的发布 QOS级别低的话就直接发送了消息
		   MessageDataService.sendPubMsg( message);
		   String iden=channel2str.get(ctx.channel());
		   saveSendMsg(messageid, iden);
	   }

		try {
			System.out.println("发布的内容为" +topname+ new String(bs, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		

	}
	
	
	/**
	 * 处理客户端过来的发布释放
	 * 
	 * 对客户端根据QOS级别发送发送完成
	 * 对订阅的上线的客户端发送消息
	 * @param ctx
	 * @param messagepub
	 */
	private void pubrel(final ChannelHandlerContext ctx,
			MqttMessage messagepub) {
		
		MqttMessageIdVariableHeader variableHeader=(MqttMessageIdVariableHeader)messagepub.variableHeader();

		final	Integer messageid=variableHeader.messageId();
		
		MqttQoS mqttQoS=messagepub.fixedHeader().qosLevel();
		
		MqttFixedHeader fixedHeader=null;
		
		if(mqttQoS.value()<=1)
			//发送发布收到  QOS级别会1
			fixedHeader	= new MqttFixedHeader(
					MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0);
		else
			//否则发送发布收到  QOS级别会2
			fixedHeader	= new MqttFixedHeader(
					MqttMessageType.PUBCOMP, false, MqttQoS.EXACTLY_ONCE, false, 0);
		
		

		MqttMessageIdVariableHeader connectVariableHeader = MqttMessageIdVariableHeader
				.from(messageid);

		MqttPubAckMessage ackMessage = new MqttPubAckMessage(fixedHeader,
				connectVariableHeader);
		ctx.writeAndFlush(ackMessage);
		
		
		TransportMessage message=messages.get(messageid);
		
		if(message!=null){
			topContent.put(message.getTopName(), messageid);			
			MessageDataService.sendPubMsg( message);
		}else{
			
			dboptgroup.submit(new Runnable() {
				
				@Override
				public void run() {
				        SqlSession session=DBSessionFactory.getSqlSession();
				        
				        MsgRepExample example=new MsgRepExample();
				        
				        example.createCriteria().andMessageidEqualTo(messageid);
				        MsgRep msgRep= session.selectOne(queryMsg, example);
				        session.close();
				        if(msgRep!=null){
				        	topContent.put(msgRep.getTopname(), messageid);	
				            TransportMessage	message=new TransportMessage();
				        	message.setMessageId(messageid);
				        	message.setContent(msgRep.getContent());
				        	MessageDataService.sendPubMsg( message);
				        }
				}
			});
		}

	}
	
	
	/**
	 *   处理客户端 发布收到
	 *   
	 *   对客户端发送发布释放
	 *   根据 客户端收到的messageid 找到相应的message  并且  存储到消息记录里面
	 * @param ctx
	 * @param messagepub
	 */
	private void pubrec(final ChannelHandlerContext ctx,
			MqttMessage messagepub) {
		
		MqttMessageIdVariableHeader variableHeader=(MqttMessageIdVariableHeader)messagepub.variableHeader();

		Integer messageid=variableHeader.messageId();
		
		
		MqttQoS mqttQoS=messagepub.fixedHeader().qosLevel();
		
		MqttFixedHeader fixedHeader=null;
		
		if(mqttQoS.value()<=1)
			//发送发布收到  QOS级别会1
			fixedHeader	= new MqttFixedHeader(
					MqttMessageType.PUBREL, false, MqttQoS.AT_MOST_ONCE, false, 0);
		else
			//否则发送发布收到  QOS级别会2
			fixedHeader	= new MqttFixedHeader(
					MqttMessageType.PUBREL, false, MqttQoS.EXACTLY_ONCE, false, 0);
		
		

		MqttMessageIdVariableHeader connectVariableHeader = MqttMessageIdVariableHeader
				.from(messageid);

		MqttPubAckMessage ackMessage = new MqttPubAckMessage(fixedHeader,
				connectVariableHeader);
		ctx.writeAndFlush(ackMessage);
		
		
		
		String iden=channel2str.get(ctx.channel());
		
		saveSendMsg(messageid, iden);

	}
	
	private  void   saveSendMsg(Integer messageid,String iden){
		 ConcurrentLinkedQueue<Integer> sendsMsgIds=messageSends.get(
				   iden);
		 
		 if(sendsMsgIds==null){
			 sendsMsgIds=new ConcurrentLinkedQueue<Integer>();
			 messageSends.put(iden, sendsMsgIds);
		 }
		 
		 TransportMessage message=messages.get(messageid);
		
       sendsMsgIds.add(message.getMessageId());
	}
	
	
	@Override
	public void channelInactive(final ChannelHandlerContext ctx)
			throws Exception {
		super.channelInactive(ctx);
		ctx.close();
	}
	
}
