package server;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Обработчик WebSocket-соединений для многопользовательского чата.
 * Основные функции:
 *  - Управление подключениями пользователей
 *  - Рассылка публичных и приватных сообщений
 *  - Контроль состояния сессий
 *  - Обработка ошибок соединения
 * Использует, потокобезопасную ConcurrentHashMap для хранения активных сессий.
 */
@WebSocket
public class WebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private static final Map<String, Session> userSessions = new ConcurrentHashMap<>();

    /**
     * Обрабатывает входящее WebSocket-соединение.
     *
     * @param session Сессия WebSocket, представляющая соединение с клиентом.
     */
    @OnWebSocketConnect
    public void onConnect(Session session) {
        logger.info("Новое подключение: {}", session.getRemoteAddress().getAddress());
        try {
            session.getRemote().sendString("Введите ваш логин:");
        } catch (IOException e) {
            logger.error("Ошибка приветствия: {}", e.getMessage());
        }
    }

    /**
     * Обрабатывает получение сообщения от клиента.
     *
     * @param session Сессия WebSocket, представляющая соединение с клиентом.
     * @param message Сообщение, полученное от клиента.
     */
    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        if (message.startsWith("LOGIN:")) {
            handleLogin(session, message);
        } else {
            String user = getUser(session);
            if (user != null) {
                broadcast(user + ": " + message, session);
            }
        }
    }

    /**
     * Обрабатывает закрытие WebSocket-соединения.
     *
     * @param session Сессия WebSocket, представляющая соединение с клиентом.
     * @param statusCode Код статуса закрытия соединения.
     * @param reason Причина закрытия соединения.
     */
    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        String user = getUser (session);
        if (user != null) {
            userSessions.remove(user);
            broadcast("Server: " + user + " покинул чат", session);
            logger.info("Пользователь {} отключился", user);
        }
    }

    /**
     * Обрабатывает возникновение ошибки в WebSocket-соединении.
     *
     * @param session Сессия WebSocket, представляющая соединение с клиентом.
     * @param error Исключение, произошедшее во время работы с соединением.
     */
    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        logger.error("WebSocket ошибка: {}", error.getMessage());
        if (session != null && session.isOpen()) {
            session.close();
        }
    }

    /**
     * Обрабатывает вход пользователя в чат.
     *
     * @param session Сессия WebSocket, представляющая соединение с клиентом.
     * @param message Сообщение, содержащее логин пользователя.
     */
    private void handleLogin(Session session, String message) {
        String username = message.split(":", 2)[1].trim();
        if (username.isEmpty()) {
            sendError(session, "Логин не может быть пустым");
            return;
        }
        if (userSessions.containsKey(username)) {
            sendError(session, "Логин уже занят");
            return;
        }
        userSessions.put(username, session);
        broadcast("Server: " + username + " подключился", session);
        logger.info("Пользователь {} авторизован", username);
    }

    /**
     * Обрабатывает отправку личного сообщения.
     *
     * @param session Сессия WebSocket, представляющая соединение с клиентом.
     * @param message Сообщение, содержащее личное сообщение.
     */
    private void handlePrivateMessage(Session session, String message) {
        String[] parts = message.split(" ", 2);
        if (parts.length < 2) {
            sendError(session, "Неверный формат личного сообщения");
            return;
        }
        String targetUser = parts[0].substring(1);
        String content = parts[1];
        sendPrivate(targetUser, "Личное от " + getUser(session) + ": " + content);
    }

    /**
     * Отправляет сообщение всем подключенным пользователям.
     *
     * @param message Сообщение, которое нужно отправить.
     */
    private void broadcast(String message, Session sender) {
        userSessions.forEach((user, session) -> {

            if (session.isOpen() && !session.equals(sender)) {
                try {
                    session.getRemote().sendString(message);
                } catch (IOException e) {
                    logger.error("Ошибка отправки: {}", e.getMessage());
                }
            }
        });

        try {
            if (sender.isOpen()) {
                sender.getRemote().sendString("Вы: " + message.split(": ", 2)[1]);
            }
        } catch (IOException e) {
            logger.error("Ошибка подтверждения: {}", e.getMessage());
        }
    }

    /**
     * Обрабатывает отправку сообщения в чат.
     *
     * @param session Сессия WebSocket, представляющая соединение с клиентом.
     * @param message Сообщение, которое нужно отправить в чат.
     */
    private void handleBroadcastMessage(Session session, String message) {
        String user = getUser(session);
        if (user == null) {
            sendError(session, "Требуется авторизация: LOGIN:ваш_логин");
            return;
        }
        broadcast(user + ": " + message, session);
        sendConfirmation(session);
    }

    /**
     * Отправляет подтверждение о доставке сообщения.
     *
     * @param session Сессия WebSocket, представляющая соединение с клиентом.
     */
    private void sendConfirmation(Session session) {
        try {
            session.getRemote().sendString("✓ Сообщение доставлено");
        } catch (IOException e) {
            logger.error("Ошибка подтверждения: {}", e.getMessage());
        }
    }

    /**
     * Отправляет личное сообщение указанному пользователю.
     *
     * @param targetUser  Имя пользователя, которому отправляется сообщение.
     * @param message Сообщение, которое нужно отправить.
     */
    private void sendPrivate(String targetUser, String message) {
        Session target = userSessions.get(targetUser);
        if (target != null && target.isOpen()) {
            try {
                target.getRemote().sendString(message);
            } catch (IOException e) {
                logger.error("Ошибка личного сообщения для {}: {}", targetUser, e.getMessage());
            }
        }
    }

    /**
     * Отправляет сообщение об ошибке клиенту.
     *
     * @param session Сессия WebSocket, представляющая соединение с клиентом.
     * @param error Сообщение об ошибке, которое нужно отправить.
     */
    private void sendError(Session session, String error) {
        try {
            session.getRemote().sendString("ERROR: " + error);
        } catch (IOException e) {
            logger.error("Ошибка отправки ошибки: {}", e.getMessage());
        }
    }

    /**
     * Получает имя пользователя, связанное с данной сессией.
     *
     * @param session Сессия WebSocket, представляющая соединение с клиентом.
     * @return Имя пользователя или null, если пользователь не авторизован.
     */
    private String getUser(Session session) {
        return userSessions.entrySet().stream()
                .filter(entry -> entry.getValue().equals(session))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}