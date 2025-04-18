package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Утилитарный класс для доступа к тестовым настройкам.
 * Параметры (базовый URL, пути) загружаются из config.properties.
 * Селекторы загружаются, из самого класса TestConfig.
 */
public class TestConfig {
    private static final Properties props = new Properties();

    static {
        try (InputStream input = TestConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) throw new RuntimeException("Файл config.properties не найден");
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка загрузки конфига", e);
        }
    }

    public static String getPageUrl(int port) {
        String base = props.getProperty("base.url");
        String path = props.getProperty("page.path");
        return base + ":" + port + path;
    }

    public static final String USERNAME_FIELD = "#username";
    public static final String ROOM_SELECTOR = "#room";
    public static final String CONNECT_BUTTON = "button:has-text('Подключиться')";
    public static final String CHAT_CONTAINER = "#chat";
    public static final String MESSAGES_AREA = "#messages";
    public static final String LAST_MESSAGE = ".message:last-child";
    public static final String MESSAGE_INPUT = "#message";
    public static final String SEND_BUTTON = "button:has-text('Отправить')";
    public static final String BODY = "body";


    public static class Users {
        public static final String PUBLIC_USER = "PublicUser";
        public static final String PRIVATE_USER = "PrivateUser";
        public static final String TEST_USER = "TestUser";
        public static final String ALICE = "Alice";
        public static final String USER1 = "User1";
        public static final String USER2 = "User2";
        public static final String HISTORIAN = "Historian";
        public static final String MULTI_USER = "MultiUser";
        public static final String RESILIENT_USER = "ResilientUser";
    }

    public static class Rooms {
        public static final String PUBLIC = "public";
        public static final String PRIVATE = "private";
    }

    public static class Messages {
        public static final String PRIVATE_MSG = "Private message";
        public static final String PUBLIC_MSG = "Public message";
        public static final String HELLO_EVERYONE = "Hello everyone!";
        public static final String CONNECTION_SUCCESS = " в комнате public подключился";
        public static final String SECRET_MSG = "Secret message";
        public static final String MESSAGE_1 = "Message 1";
        public static final String ERROR_LOGIN_EXISTS = "ERROR: Логин уже занят";
    }

    public static class History {
        public static final String HISTORY_BUTTON = "button:has-text('История сообщений')";
    }

    public static class Connection {
        public static final String WS_CLOSE_SCRIPT = "() => ws.close()";
    }
}