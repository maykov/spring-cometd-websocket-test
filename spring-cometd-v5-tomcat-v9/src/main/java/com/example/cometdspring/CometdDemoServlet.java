package com.example.cometdspring;

import static java.util.Objects.nonNull;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import org.cometd.annotation.Listener;
import org.cometd.annotation.Service;
import org.cometd.annotation.server.ServerAnnotationProcessor;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.Promise;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.BayeuxServerImpl;
import org.cometd.server.LocalSessionImpl;
import org.cometd.server.authorizer.GrantAuthorizer;
import org.cometd.server.websocket.javax.WebSocketTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CometdDemoServlet extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(CometdDemoServlet.class);
	public static CometdDemoServlet fThis = null;
	public static LocalSession fLocalSession;

	@Override
	public void init() throws ServletException {
		super.init();
		fThis = this;

		BayeuxServerImpl bayeux = getBayeuxServer();

		if (bayeux == null) {
			throw new UnavailableException("No BayeuxServer!");
		}

		bayeux.addExtension(new SendListener());
		bayeux.addListener(new SessionListener());

		bayeux.createChannelIfAbsent("/**", channel -> channel.addAuthorizer(GrantAuthorizer.GRANT_ALL));

		bayeux.getChannel(ServerChannel.META_HANDSHAKE).addAuthorizer(GrantAuthorizer.GRANT_PUBLISH);

		bayeux.setTransports(new WebSocketTransport(bayeux));

		fLocalSession = new LocalSessionImpl(getBayeuxServer(), "local");
		fLocalSession.handshake();

		ServerAnnotationProcessor processor = new ServerAnnotationProcessor(bayeux);

		processor.process(new Monitor());
//		processor.process(new DemoRPC());

	}

	BayeuxServerImpl getBayeuxServer() {
		return (BayeuxServerImpl) getServletContext().getAttribute(BayeuxServer.ATTRIBUTE);
	}

	public static class SessionListener implements BayeuxServer.SessionListener {

		@Override
		public void sessionAdded(ServerSession session, ServerMessage message) {
			fThis.getBayeuxServer().getSession(session.getId()).addListener(new MessageListener());
		}

		@Override
		public void sessionRemoved(ServerSession session, boolean timedout) {

		}
	}

	public static class MessageListener implements ServerSession.MessageListener {

		@Override
		public boolean onMessage(ServerSession session, ServerSession sender, ServerMessage message) {
			logger.info("Sending message: [" + message.get("data") + "] from " + sender.getId() + " to " + session.getId());
			return true;
		}
	}

	public static class SendListener implements BayeuxServer.Extension {
		@Override
		public void incoming(ServerSession from, ServerMessage.Mutable message, Promise<Boolean> promise) {
			BayeuxServer.Extension.super.incoming(from, message, promise);
			if (nonNull(from) && ((String)message.get("channel")).contains("demo"))
				logger.info("Incoming message: [" + message.get("data") + "] from " + from.getId());
		}

	}

	@Service("monitor")
	public static class Monitor {
		@Listener("/meta/subscribe")
		public void monitorSubscribe(ServerSession session, ServerMessage message) {
			logger.info("Monitored Subscribe from " + session + " for " + message.get(Message.SUBSCRIPTION_FIELD));
		}

		@Listener("/meta/unsubscribe")
		public void monitorUnsubscribe(ServerSession session, ServerMessage message) {
			logger.info("Monitored Unsubscribe from " + session + " for " + message.get(Message.SUBSCRIPTION_FIELD));
		}
	}

	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		((HttpServletResponse)res).sendError(503);
	}
}