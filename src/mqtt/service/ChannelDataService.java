package mqtt.service;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author tzj
 *
 */
public class ChannelDataService {

  
	static ConcurrentHashMap<String, Channel> str2channel=new ConcurrentHashMap<String, Channel>();

	static	ConcurrentHashMap<Channel, String> channel2str=new ConcurrentHashMap<Channel, String>();

	public static ConcurrentHashMap<String, Channel> getStr2channel() {
		return str2channel;
	}

	public static ConcurrentHashMap<Channel, String> getChannel2str() {
		return channel2str;
	}

	public static void setChannel2str(ConcurrentHashMap<Channel, String> channel2str) {
		ChannelDataService.channel2str = channel2str;
	}

	public static void setStr2channel(ConcurrentHashMap<String, Channel> str2channel) {
		ChannelDataService.str2channel = str2channel;
	}

	
	 
	
	
	
}
