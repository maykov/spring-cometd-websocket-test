package org.example.client;


import static java.util.Collections.singletonList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

import org.cometd.bayeux.Channel;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.benchmark.Config;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.common.JacksonJSONContextClient;
import org.cometd.websocket.client.WebSocketTransport;

public class Client {
	private static final Logger logger = Logger.getLogger("CometdClient");
	private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();
	private static final WebSocketContainer WS_CONTAINER = ContainerProvider.getWebSocketContainer();
	private static final String ORIGIN = "http://localhost:8085";
	private static final String WS_ORIGIN = "ws://localhost:8085/cometd";
	private static final String HOST = "localhost:8085";
	private Semaphore startSignal = new Semaphore(1);

	private Map<String, ClientSessionChannel> channelNameToClientSessionChannel = new HashMap<>();

	public volatile boolean connected = false;

	private final BayeuxClient bayeuxClient;

	public Client() {
		bayeuxClient = new BayeuxClient(WS_ORIGIN, getWebSocketTransport());
		bayeuxClient.getChannel(Channel.META_HANDSHAKE).addListener((ClientSessionChannel.MessageListener) (clientSessionChannel, message) -> {
			if (message.isSuccessful()) {
				logger.info("Handshake - is successful");
			} else {
				logger.info("Handshake - failed");
			}

			startSignal.release();
		});

		bayeuxClient.getChannel(Channel.META_DISCONNECT).addListener((ClientSessionChannel.MessageListener) (clientSessionChannel, message) -> {
			logger.info("disconnect: " + message);
		});
	}

	public void start() {
		try {
			startSignal.acquire();
			bayeuxClient.handshake();
		} catch (InterruptedException e) {
			startSignal.release();
			throw new RuntimeException(e);
		}

		connect();
	}

	private void connect() {
		if (connected)
			return;

		try {
			startSignal.acquire();
		} catch (InterruptedException e) {
			logger.warning("Failed to connect: " + e);
			startSignal.release();
			throw new RuntimeException(e);
		}

		bayeuxClient.getChannel(Channel.META_CONNECT).addListener((ClientSessionChannel.MessageListener) (clientSessionChannel, message) -> {
			if (message.isSuccessful()) {
				if (!connected) {
					connected = true;
					startSignal.release();
					logger.info("Connected successfully");
				}
			} else {
				connected = false;
				logger.warning("failed to connect");
			}
		});
	}

	void stop() {
		logger.info("disconnecting");
		bayeuxClient.disconnect(30_000);
		startSignal.release();
	}

	public void subscribe(String channelName) {
		//TODO: why are we acquiring the lock like this ?
		if (!connected) {
			try {
				startSignal.acquire();
				if (connected) {
					startSignal.release();
				}
			} catch (InterruptedException e) {
				logger.warning("subscribeToChannel : " + channelName + ", failed with exception : " + e);
				startSignal.release();
				throw new RuntimeException(e);
			}
		}

		logger.info("Subscribing to " + channelName);
		if (!channelNameToClientSessionChannel.containsKey(channelName)) {
			ClientSessionChannel channel = bayeuxClient.getChannel(channelName);
			channel.subscribe((clientSessionChannel, message) -> {
				logger.info("Message Listener - " + message);
			}, message -> {
				logger.info("Message Subscription Listener - " + message);
			});

			channelNameToClientSessionChannel.put(channelName, channel);
		}
	}

	public void publish(String channelName, Map<String, Object> message) {
		if (!channelNameToClientSessionChannel.containsKey(channelName)) {
			logger.warning("Channel " + channelName + ", not subscribed. Not publishing message.");
			return;
		}

		logger.info("Publishing message " + message + " on channel " + channelName);
		channelNameToClientSessionChannel.get(channelName).publish(message, message1 -> {
			logger.info("Channel " + channelName + ", publish result." + message1);
		});
	}

	private WebSocketTransport getWebSocketTransport() {
		Map<String, Object> options = new HashMap<>();
		options.put(ClientTransport.JSON_CONTEXT_OPTION, new JacksonJSONContextClient());
		options.put(ClientTransport.MAX_NETWORK_DELAY_OPTION, Config.MAX_NETWORK_DELAY);
		// Differently from HTTP where the idle timeout is adjusted if it is a /meta/connect
		// for WebSocket we need an idle timeout that is longer than the /meta/connect timeout.
		options.put(WebSocketTransport.IDLE_TIMEOUT_OPTION, Config.META_CONNECT_TIMEOUT + 10_000 + 10_000);
		return new WebSocketTransport(options, SCHEDULER, WS_CONTAINER) {
			@Override
			protected void onHandshakeRequest(Map<String, List<String>> headers) {
				headers.put("Origin", singletonList(ORIGIN));
				headers.put("Host", singletonList(HOST));
				super.onHandshakeRequest(headers);
			}
		};
	}

}