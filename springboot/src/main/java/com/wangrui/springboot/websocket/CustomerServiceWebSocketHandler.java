package com.wangrui.springboot.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangrui.springboot.mapper.CustomerServiceMessageMapper;
import com.wangrui.springboot.pojo.CustomerServiceMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CustomerServiceWebSocketHandler extends TextWebSocketHandler {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_MESSAGE_LENGTH = 500;

    private final CustomerServiceMessageMapper messageMapper;
    private final Map<Integer, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final Map<Integer, WebSocketSession> serviceSessions = new ConcurrentHashMap<>();

    public CustomerServiceWebSocketHandler(CustomerServiceMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        if ("SERVICE".equals(session.getAttributes().get("role"))) {
            WebSocketSession previousSession = serviceSessions.put(getUserId(session), session);
            closePreviousSession(previousSession);
            sendSystemMessage(session, "客服工作台已连接。");
            return;
        }

        WebSocketSession previousSession = userSessions.put(getUserId(session), session);
        closePreviousSession(previousSession);
        sendSystemMessage(session, serviceSessions.isEmpty()
                ? "客服当前可能不在线，消息会先保存，客服上线后可以查看。"
                : "客服已连接，请描述您遇到的问题。");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode payload = OBJECT_MAPPER.readTree(message.getPayload());
        String type = payload.path("type").asText("").toUpperCase();

        if ("HEARTBEAT".equals(type)) {
            sendSystemMessage(session, "连接正常");
            return;
        }

        String role = String.valueOf(session.getAttributes().get("role"));
        if ("USER".equals(role) && "CHAT".equals(type)) {
            handleUserMessage(session, payload);
            return;
        }
        if ("SERVICE".equals(role) && "SERVICE_REPLY".equals(type)) {
            handleServiceMessage(session, payload);
            return;
        }

        sendSystemMessage(session, "不支持的消息类型或当前角色无权发送该消息");
    }

    private void handleUserMessage(WebSocketSession session, JsonNode payload) {
        String content = payload.path("content").asText("").trim();
        if (content.isEmpty()) {
            sendSystemMessage(session, "消息内容不能为空");
            return;
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            sendSystemMessage(session, "单条消息不能超过 500 个字符");
            return;
        }

        CustomerServiceMessage savedMessage = saveMessage(getUserId(session), "USER", content);
        sendChatMessage(session, "USER", savedMessage, getUsername(session));
        broadcastToServices(savedMessage, getUsername(session));
    }

    private void handleServiceMessage(WebSocketSession session, JsonNode payload) {
        Integer targetUserId = payload.path("userId").canConvertToInt()
                ? payload.path("userId").asInt()
                : null;
        String content = payload.path("content").asText("").trim();

        if (targetUserId == null || targetUserId <= 0) {
            sendSystemMessage(session, "请先选择要回复的用户");
            return;
        }
        if (content.isEmpty()) {
            sendSystemMessage(session, "消息内容不能为空");
            return;
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            sendSystemMessage(session, "单条消息不能超过 500 个字符");
            return;
        }

        CustomerServiceMessage savedMessage = saveMessage(targetUserId, "SERVICE", content);
        WebSocketSession userSession = userSessions.get(targetUserId);
        if (userSession != null) {
            sendChatMessage(userSession, "SERVICE", savedMessage, getUsername(session));
        }
        broadcastToServices(savedMessage, getUsername(session));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        if ("SERVICE".equals(session.getAttributes().get("role"))) {
            serviceSessions.remove(getUserId(session), session);
            return;
        }
        userSessions.remove(getUserId(session), session);
    }

    private CustomerServiceMessage saveMessage(Integer userId, String sender, String content) {
        CustomerServiceMessage chatMessage = new CustomerServiceMessage();
        chatMessage.setUserId(userId);
        chatMessage.setSender(sender);
        chatMessage.setContent(content);
        chatMessage.setCreatedAt(new Date());
        messageMapper.insert(chatMessage);
        return chatMessage;
    }

    private void broadcastToServices(CustomerServiceMessage message) {
        broadcastToServices(message, null);
    }

    private void broadcastToServices(CustomerServiceMessage message, String serviceUsername) {
        serviceSessions.values().forEach(serviceSession ->
                sendChatMessage(serviceSession, message.getSender(), message, serviceUsername)
        );
    }

    private void sendSystemMessage(WebSocketSession session, String content) {
        try {
            if (!session.isOpen()) {
                return;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "SYSTEM");
            payload.put("content", content);
            payload.put("sentAt", System.currentTimeMillis());
            synchronized (session) {
                session.sendMessage(new TextMessage(OBJECT_MAPPER.writeValueAsString(payload)));
            }
        } catch (IOException ignored) {
            closeQuietly(session);
        }
    }

    private void sendChatMessage(WebSocketSession session,
                                 String messageType,
                                 CustomerServiceMessage message,
                                 String serviceUsername) {
        try {
            if (!session.isOpen()) {
                return;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", messageType);
            payload.put("id", message.getId());
            payload.put("userId", message.getUserId());
            payload.put("sender", message.getSender());
            payload.put("content", message.getContent());
            payload.put("sentAt", message.getCreatedAt().getTime());
            payload.put("senderUsername", serviceUsername);
            synchronized (session) {
                session.sendMessage(new TextMessage(OBJECT_MAPPER.writeValueAsString(payload)));
            }
        } catch (IOException ignored) {
            closeQuietly(session);
        }
    }

    private Integer getUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId instanceof Integer integer ? integer : -1;
    }

    private String getUsername(WebSocketSession session) {
        Object username = session.getAttributes().get("username");
        return username != null ? username.toString() : "客服";
    }

    private void closePreviousSession(WebSocketSession previousSession) {
        if (previousSession != null && previousSession.isOpen()) {
            closeQuietly(previousSession);
        }
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close();
        } catch (IOException ignored) {
            // 连接已断开时可忽略关闭异常。
        }
    }
}
