package org.example.client;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
	private static final String CHANNEL_NAME = "/demo/";
	private static final int NUMBER_OF_CLIENTS = 200;
	private static final long SLEEPTIME_IN_SECONDS_BETWEEN_MESSAGES = 10;
	private static final long HOW_LONG_TO_RUN_IN_SECONDS = 3600; //1 hour
	public static void main(String[] args) throws Exception {

		Map<Integer, Client> clientMap = new HashMap<>();

//		Subscribe to each channel and immediately publish
		for (int i = 0; i < NUMBER_OF_CLIENTS; i++) {
			Client cometdClient = new Client();
			cometdClient.start();
			cometdClient.subscribe(CHANNEL_NAME + i);
			Map<String, Object> message = new HashMap<>();
			message.put("Message", "Hello World!");
			cometdClient.publish(CHANNEL_NAME + i, message);
			clientMap.put(i, cometdClient);
		}

		long currentTime = System.currentTimeMillis();
		long endTime = currentTime + HOW_LONG_TO_RUN_IN_SECONDS * 1000;

		//Run for 1 hour
		AtomicInteger round = new AtomicInteger(0);
		while (currentTime < endTime) {
			Thread.sleep(SLEEPTIME_IN_SECONDS_BETWEEN_MESSAGES * 1000);
			clientMap.forEach((clientId, client) -> {
				Map<String, Object> message = new HashMap<>();
				message.put("Message", "Hello World! " + round + " " + new Date());
				client.publish(CHANNEL_NAME + clientId, message);
			});
			currentTime = System.currentTimeMillis();
			round.incrementAndGet();
		}

		clientMap.forEach((clientId, client) -> client.stop());
	}
}
