package mqtt.entity;

/**
 * 消息类
 * @author acer
 *
 */
public class TransportMessage {
	
	/**
	 * 消息id
	 */
	int messageId;
	
	/**
	 * 消息主题
	 */
	String topName;
	
	/**
	 * 消息内容
	 */
	byte[] content;

	
	public int getMessageId() {
		return messageId;
	}

	public void setMessageId(int messageId) {
		this.messageId = messageId;
	}

	public String getTopName() {
		return topName;
	}

	public void setTopName(String topName) {
		this.topName = topName;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte[] content) {
		this.content = content;
	}
	
	

}
