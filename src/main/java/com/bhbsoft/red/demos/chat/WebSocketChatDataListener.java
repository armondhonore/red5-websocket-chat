package com.bhbsoft.red.demos.chat;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import org.red5.logging.Red5LoggerFactory;
import org.red5.net.websocket.Constants;
import org.red5.net.websocket.WebSocketConnection;
import org.red5.net.websocket.listener.WebSocketDataListener;
import org.red5.net.websocket.model.MessageType;
import org.red5.net.websocket.model.WSMessage;
import org.slf4j.Logger;

/**
 * Handler / router for chat data.
 * 
 * @author Paul Gregoire
 */
public class WebSocketChatDataListener extends WebSocketDataListener {

	private static final Logger log = Red5LoggerFactory.getLogger(WebSocketChatDataListener.class, "chat");

	{
		setProtocol("chat");
	}

	private Set<WebSocketConnection> connections = new HashSet<WebSocketConnection>();
	
	@Override
	public void onWSConnect(WebSocketConnection conn) {
		log.info("Connect: {}", conn);
		if (conn.getHeaders().containsKey(Constants.WS_HEADER_PROTOCOL)) {
			String protocol = (String) conn.getHeaders().get(Constants.WS_HEADER_PROTOCOL);
			if (protocol.indexOf("chat") != -1) {
				log.debug("Chat enabled");
			} else {
				log.info("Chat is not in the connections protocol list");
			}
		}
		connections.add(conn);
	}

	@Override
	public void onWSDisconnect(WebSocketConnection conn) {
		log.info("Disconnect: {}", conn);
		connections.remove(conn);
	}

	@Override
	public void onWSMessage(WSMessage message) {
		// if its protocol doesn't match then skip the message
		if (!protocol.equals(message.getConnection().getProtocol())) {
			log.debug("Skipping message due to protocol mismatch");
			return;
		}
		// ignore ping and pong
		if (message.getMessageType() == MessageType.PING || message.getMessageType() == MessageType.PONG) {
			return;
		}
		// close if we get a close
		if (message.getMessageType() == MessageType.CLOSE) {
			message.getConnection().close();
			return;
		}
		// assume we have text
		String msg = new String(message.getPayload().array()).trim();
		log.info("onWSMessage: {}\n{}", msg, message.getConnection());
		// do a quick json check
		if (msg.indexOf('{') != -1 && msg.indexOf(':') != -1) {
			log.info("JSON encoded text message");
			// channelName == roomid in most cases
			JSONObject obj = null;
			JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
			try {
				obj = (JSONObject) parser.parse(msg);
				log.debug("Parsed - keys: {}\ncontent: {}", obj.keySet(), obj);
				for (WebSocketConnection conn : connections) {
					try {
						conn.send(JSONValue.toJSONString(obj));
					} catch (UnsupportedEncodingException e) {
					}
				}
			} catch (ParseException e) {
				log.warn("Exception parsing JSON", e);
			}
		} else {
			log.info("Standard text message");
			for (WebSocketConnection conn : connections) {
				try {
					conn.send(msg);
				} catch (UnsupportedEncodingException e) {
				}
			}
		}
	}

}
