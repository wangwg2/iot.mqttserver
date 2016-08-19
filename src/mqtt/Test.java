package mqtt;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	
		
		MqttServer mqttServer= new MqttServer();
		
		try {
			mqttServer.start();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
