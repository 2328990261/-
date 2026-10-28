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

/**
 * 客户服务 WebSocket 消息处理器
 * 管理用户与客服的实时在线会话，转发聊天消息、心跳与系统提示，并落库保存
 */
@Component
public class CustomerServiceWebSocketHandler extends TextWebSocketHandler {

    // JSON 序列化工具（并发安全，作为单例复用）
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    // 单条消息最大长度
    private static final int MAX_MESSAGE_LENGTH = 500;

    // 消息持久化 Mapper
    private final CustomerServiceMessageMapper messageMapper;
    // 在线用户会话映射：userId -> session
    private final Map<Integer, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    // 在线客服会话映射：userId -> session
    private final Map<Integer, WebSocketSession> serviceSessions = new ConcurrentHashMap<>();

    // 构造方法：注入消息 Mapper
    public CustomerServiceWebSocketHandler(CustomerServiceMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    /**
     * 连接建立后：登记会话，关闭旧连接并发送系统欢迎/提示消息
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 1. 若为客服角色，登记客服会话并提示已连接
        if ("SERVICE".equals(session.getAttributes().get("role"))) {
            WebSocketSession previousSession = serviceSessions.put(getUserId(session), session);
            closePreviousSession(previousSession);
            sendSystemMessage(session, "客服工作台已连接。");
            return;
        }

        // 2. 否则登记用户会话并提示客服是否在线
        WebSocketSession previousSession = userSessions.put(getUserId(session), session);
        closePreviousSession(previousSession);
        sendSystemMessage(session, serviceSessions.isEmpty()
                ? "客服当前可能不在线，消息会先保存，客服上线后可以查看。"
                : "客服已连接，请描述您遇到的问题。");
    }

    /**
     * 处理收到的文本消息：区分心跳、用户聊天、客服回复三类
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 1. 解析消息结构
        JsonNode payload = OBJECT_MAPPER.readTree(message.getPayload());
        String type = payload.path("type").asText("").toUpperCase();

        // 2. 心跳消息：回复连接正常
        if ("HEARTBEAT".equals(type)) {
            sendSystemMessage(session, "连接正常");
            return;
        }

        // 3. 根据角色分发处理
        String role = String.valueOf(session.getAttributes().get("role"));
        if ("USER".equals(role) && "CHAT".equals(type)) {
            handleUserMessage(session, payload);
            return;
        }
        if ("SERVICE".equals(role) && "SERVICE_REPLY".equals(type)) {
            handleServiceMessage(session, payload);
            return;
        }

        // 4. 未匹配的消息类型/权限则提示
        sendSystemMessage(session, "不支持的消息类型或当前角色无权发送该消息");
    }

    /** 处理用户聊天消息：校验、落库、反馈给用户并广播给在线客服 */
    private void handleUserMessage(WebSocketSession session, JsonNode payload) {
        // 1. 校验消息内容
        String content = payload.path("content").asText("").trim();
        if (content.isEmpty()) {
            sendSystemMessage(session, "消息内容不能为空");
            return;
        }
        if (content.length() > MAX_MESSAGE_LENGTH) {
            sendSystemMessage(session, "单条消息不能超过 500 个字符");
            return;
        }

        // 2. 保存消息到数据库
        CustomerServiceMessage savedMessage = saveMessage(getUserId(session), "USER", content);
        // 3. 回显给发送者本人
        sendChatMessage(session, "USER", savedMessage, getUsername(session));
        // 4. 广播给所有在线客服
        broadcastToServices(savedMessage, getUsername(session));
    }

    /** 处理客服回复：校验目标用户与内容、落库、推送给用户并广播给客服 */
    private void handleServiceMessage(WebSocketSession session, JsonNode payload) {
        // 1. 解析目标用户ID
        Integer targetUserId = payload.path("userId").canConvertToInt()
                ? payload.path("userId").asInt()
                : null;
        String content = payload.path("content").asText("").trim();

        // 2. 校验目标用户与内容
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

        // 3. 保存消息到数据库
        CustomerServiceMessage savedMessage = saveMessage(targetUserId, "SERVICE", content);
        // 4. 若目标用户在线，实时推送
        WebSocketSession userSession = userSessions.get(targetUserId);
        if (userSession != null) {
            sendChatMessage(userSession, "SERVICE", savedMessage, getUsername(session));
        }
        // 5. 广播给其他客服（保持多端同步）
        broadcastToServices(savedMessage, getUsername(session));
    }

    /**
     * 连接关闭后：从对应会话映射中移除
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 1. 客服断开则移除客服会话
        if ("SERVICE".equals(session.getAttributes().get("role"))) {
            serviceSessions.remove(getUserId(session), session);
            return;
        }
        // 2. 用户断开则移除用户会话
        userSessions.remove(getUserId(session), session);
    }

    /** 保存聊天消息到数据库并返回带ID的完整消息 */
    private CustomerServiceMessage saveMessage(Integer userId, String sender, String content) {
        // 1. 组装消息实体
        CustomerServiceMessage chatMessage = new CustomerServiceMessage();
        chatMessage.setUserId(userId);
        chatMessage.setSender(sender);
        chatMessage.setContent(content);
        chatMessage.setCreatedAt(new Date());
        // 2. 写入数据库
        messageMapper.insert(chatMessage);
        return chatMessage;
    }

    /** 广播消息给全部在线客服（不带客服用户名） */
    private void broadcastToServices(CustomerServiceMessage message) {
        broadcastToServices(message, null);
    }

    /** 广播消息给全部在线客服，附带发送者用户名 */
    private void broadcastToServices(CustomerServiceMessage message, String serviceUsername) {
        // 遍历每个在线客服会话并推送消息
        serviceSessions.values().forEach(serviceSession ->
                sendChatMessage(serviceSession, message.getSender(), message, serviceUsername)
        );
    }

    /** 发送系统提示消息（SYSTEM 类型） */
    private void sendSystemMessage(WebSocketSession session, String content) {
        try {
            // 1. 会话未打开则忽略
            if (!session.isOpen()) {
                return;
            }
            // 2. 组装系统消息负载
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "SYSTEM");
            payload.put("content", content);
            payload.put("sentAt", System.currentTimeMillis());
            // 3. 加锁发送，保证顺序
            synchronized (session) {
                session.sendMessage(new TextMessage(OBJECT_MAPPER.writeValueAsString(payload)));
            }
        } catch (IOException ignored) {
            closeQuietly(session);
        }
    }

    /** 发送聊天消息（用户/客服类型），附带发送者用户名 */
    private void sendChatMessage(WebSocketSession session,
                                 String messageType,
                                 CustomerServiceMessage message,
                                 String serviceUsername) {
        try {
            // 1. 会话未打开则忽略
            if (!session.isOpen()) {
                return;
            }
            // 2. 组装聊天消息负载
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", messageType);
            payload.put("id", message.getId());
            payload.put("userId", message.getUserId());
            payload.put("sender", message.getSender());
            payload.put("content", message.getContent());
            payload.put("sentAt", message.getCreatedAt().getTime());
            payload.put("senderUsername", serviceUsername);
            // 3. 加锁发送，保证顺序
            synchronized (session) {
                session.sendMessage(new TextMessage(OBJECT_MAPPER.writeValueAsString(payload)));
            }
        } catch (IOException ignored) {
            closeQuietly(session);
        }
    }

    /** 从会话属性获取用户ID，缺失时返回-1 */
    private Integer getUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId instanceof Integer integer ? integer : -1;
    }

    /** 从会话属性获取用户名，缺失时默认"客服" */
    private String getUsername(WebSocketSession session) {
        Object username = session.getAttributes().get("username");
        return username != null ? username.toString() : "客服";
    }

    /** 关闭旧的重复连接 */
    private void closePreviousSession(WebSocketSession previousSession) {
        if (previousSession != null && previousSession.isOpen()) {
            closeQuietly(previousSession);
        }
    }

    /** 静默关闭会话，忽略已断开导致的异常 */
    private void closeQuietly(WebSocketSession session) {
        try {
            session.close();
        } catch (IOException ignored) {
            // 连接已断开时可忽略关闭异常。
        }
    }
}