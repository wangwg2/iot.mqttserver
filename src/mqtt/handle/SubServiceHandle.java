package mqtt.handle;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttSubAckPayload;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import mqtt.entity.TransportMessage;
import mqtt.service.ChannelDataService;
import mqtt.service.MessageDataService;

/**
 * 处理订阅的handle
 * @author acer
 *
 */
public class SubServiceHandle extends  ChannelInboundHandlerAdapter {

	ConcurrentHashMap<Channel, String> channel2str;
	
	

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
	/**
	 * 每一个客户端订阅的主题
	 */
	ConcurrentHashMap<String, List<String>> submap;
	

	
	
	public SubServiceHandle(){
		
		messages=MessageDataService.getMessages();
		messageSends=MessageDataService.getMessageSends();
		topContent=MessageDataService.getTopContent();
		
		submap=MessageDataService.getSubmap();
		channel2str=ChannelDataService.getChannel2str();
		
		
	}
	
	public void channelRead(ChannelHandlerContext ctx, Object msg) {

		if (msg instanceof MqttMessage) {

			MqttMessage message = (MqttMessage) msg;
			MqttMessageType messageType = message.fixedHeader().messageType();

			switch (messageType) {
			case SUBSCRIBE:
				MqttSubscribeMessage subscribeMessage = (MqttSubscribeMessage) message;
				sub(ctx, subscribeMessage);
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
	 * 订阅
	 * 
	 * @param ctx
	 * @param subscribeMessage
	 */
	private void sub(final ChannelHandlerContext ctx,
			MqttSubscribeMessage subscribeMessage) {

	
		MqttFixedHeader fixedHeader = new MqttFixedHeader(
				MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);

		MqttSubAckPayload payload = new MqttSubAckPayload(2);
		MqttMessageIdVariableHeader mqttMessageIdVariableHeader = MqttMessageIdVariableHeader
				.from(subscribeMessage.variableHeader().messageId());
		MqttSubAckMessage subAckMessage = new MqttSubAckMessage(fixedHeader,
				mqttMessageIdVariableHeader, payload);
		ctx.writeAndFlush(subAckMessage);

		List<MqttTopicSubscription> list = subscribeMessage.payload()
				.topicSubscriptions();

		List<String> strings = new ArrayList<String>();
		for (MqttTopicSubscription subscription : list) {
			strings.add(subscription.topicName());
		}

		String iden=channel2str.get(ctx.channel());
		submap.put(iden, strings);
		
		
		for (String topname : strings) {
			if (topContent.containsKey(topname)){
				TransportMessage message=messages.get(topContent.get(topname));
				
				if(message==null)
					continue;
				
				ConcurrentLinkedQueue<Integer> integers=messageSends.get(iden);
				 if(integers!=null&&integers.contains(message.getMessageId())){
					 continue;
				 }
				 MessageDataService.sendPubMsg(message);
					
			}
		}
		

	}
	@Override
	public void channelInactive(final ChannelHandlerContext ctx)
			throws Exception {
		super.channelInactive(ctx);
		ctx.close();
	}
}
