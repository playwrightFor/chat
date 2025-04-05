package server;

import com.microsoft.playwright.*;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import config.TestConfig;
import org.junit.jupiter.api.*;

import java.util.List;


/**
 * Класс WebSocketChatTests содержит автотесты для функциональности чата на основе WebSocket.
 * Тесты проверяют различные сценарии взаимодействия пользователей в публичных и приватных комнатах,
 * включая успешное подключение, отправку и получение сообщений, а также управление историей сообщений.
 * Класс использует Playwright для автоматизации браузера и JUnit 5 для организации тестов.
 * Тесты запускаются в контексте браузера, который инициализируется и закрывается перед и после всех тестов.
 */
public class WebSocketChatTests {
    private static Playwright playwright;
    private static ServerManager server;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    @BeforeAll
    static void setupAll() {
        playwright = Playwright.create();
        server = new ServerManager();
        server.start();
    }

    @AfterAll
    static void tearDownAll() {
        playwright.close();
        server.stop();
    }

    @BeforeEach
    void setup() {
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false).setSlowMo(500));

        context = browser.newContext();

        context.grantPermissions(List.of("notifications"));

        page = context.newPage();

        page.navigate(TestConfig.getPageUrl(server.getPort()));

        page.waitForSelector(TestConfig.USERNAME_FIELD, new Page.WaitForSelectorOptions().setTimeout(5000));
    }


    @AfterEach
    void tearDown() {
        context.close();
        browser.close();
    }


    @Test
    @DisplayName("Сообщения из приватной комнаты не видны в публичной")
    void testPublicRoomDoesNotReceivePrivateMessages() {
        page.fill(TestConfig.USERNAME_FIELD, TestConfig.Users.PUBLIC_USER);
        page.selectOption(TestConfig.ROOM_SELECTOR, TestConfig.Rooms.PUBLIC);
        page.click(TestConfig.CONNECT_BUTTON);
        page.waitForSelector(TestConfig.CHAT_CONTAINER);

        Page privateUserPage = context.newPage();
        privateUserPage.navigate(TestConfig.getPageUrl(server.getPort()));
        privateUserPage.fill(TestConfig.USERNAME_FIELD, TestConfig.Users.PRIVATE_USER);
        privateUserPage.selectOption(TestConfig.ROOM_SELECTOR, TestConfig.Rooms.PRIVATE);
        privateUserPage.click(TestConfig.CONNECT_BUTTON);
        privateUserPage.waitForSelector(TestConfig.CHAT_CONTAINER);

        privateUserPage.fill("#message", TestConfig.Messages.PRIVATE_MSG);
        privateUserPage.click("button:has-text('Отправить')");

        PlaywrightAssertions.assertThat(page.locator(TestConfig.MESSAGES_AREA))
                .not().containsText(TestConfig.Messages.PUBLIC_MSG);
    }

    @Test
    @DisplayName("Успешное подключение пользователя к серверу")
    void testSuccessfulConnection() {
        page.fill(TestConfig.USERNAME_FIELD, TestConfig.Users.TEST_USER);
        page.selectOption(TestConfig.ROOM_SELECTOR, TestConfig.Rooms.PUBLIC);
        page.click(TestConfig.CONNECT_BUTTON);

        PlaywrightAssertions.assertThat(page.locator(TestConfig.MESSAGES_AREA))
                .containsText(TestConfig.Users.TEST_USER + TestConfig.Messages.CONNECTION_SUCCESS);
    }


    @Test
    @DisplayName("Сообщение рассылается всем участникам публичной комнаты")
    void testMessageBroadcast() {
        page.fill(TestConfig.USERNAME_FIELD, TestConfig.Users.ALICE);
        page.selectOption(TestConfig.ROOM_SELECTOR, TestConfig.Rooms.PUBLIC);
        page.click(TestConfig.CONNECT_BUTTON);

        page.fill(TestConfig.MESSAGE_INPUT, TestConfig.Messages.HELLO_EVERYONE);
        page.click(TestConfig.SEND_BUTTON);

        PlaywrightAssertions.assertThat(page.locator(TestConfig.LAST_MESSAGE))
                .containsText(TestConfig.Messages.HELLO_EVERYONE);
    }


    @Test
    @DisplayName("Участники приватной комнаты получают сообщения друг от друга")
    void testPrivateRoomCommunication() {
        page.fill(TestConfig.USERNAME_FIELD, TestConfig.Users.USER1);
        page.selectOption(TestConfig.ROOM_SELECTOR, TestConfig.Rooms.PRIVATE);
        page.click(TestConfig.CONNECT_BUTTON);

        Page user2Page = context.newPage();
        user2Page.navigate(TestConfig.getPageUrl(server.getPort()));
        user2Page.fill(TestConfig.USERNAME_FIELD, TestConfig.Users.USER2);
        user2Page.selectOption(TestConfig.ROOM_SELECTOR, TestConfig.Rooms.PRIVATE);
        user2Page.click(TestConfig.CONNECT_BUTTON);

        user2Page.fill(TestConfig.MESSAGE_INPUT, TestConfig.Messages.SECRET_MSG);
        user2Page.click(TestConfig.SEND_BUTTON);

        PlaywrightAssertions.assertThat(page.locator(TestConfig.LAST_MESSAGE))
                .containsText(TestConfig.Messages.SECRET_MSG);
    }


    @Test
    @DisplayName("История сообщений сохраняется после отправки")
    void testMessageHistory() {
        page.fill(TestConfig.USERNAME_FIELD, TestConfig.Users.HISTORIAN);
        page.selectOption(TestConfig.ROOM_SELECTOR, TestConfig.Rooms.PUBLIC);
        page.click(TestConfig.CONNECT_BUTTON);

        page.fill(TestConfig.MESSAGE_INPUT, TestConfig.Messages.MESSAGE_1);
        page.click(TestConfig.SEND_BUTTON);

        Page historyPage = context.waitForPage(() -> {
            page.click(TestConfig.History.HISTORY_BUTTON);
        });

        PlaywrightAssertions.assertThat(historyPage.locator(TestConfig.BODY))
                .containsText(TestConfig.Messages.MESSAGE_1);
    }


    @Test
    @DisplayName("Приватная комната получает свои сообщения (негативный сценарий)")
    void testPrivateRoomReceivesMessages() {
        page.fill(TestConfig.USERNAME_FIELD, TestConfig.Users.PUBLIC_USER);
        page.selectOption(TestConfig.ROOM_SELECTOR, TestConfig.Rooms.PUBLIC);
        page.click(TestConfig.CONNECT_BUTTON);

        Page privateUserPage = context.newPage();
        privateUserPage.navigate(TestConfig.getPageUrl(server.getPort()));
        privateUserPage.fill(TestConfig.USERNAME_FIELD, TestConfig.Users.PRIVATE_USER);
        privateUserPage.selectOption(TestConfig.ROOM_SELECTOR, TestConfig.Rooms.PRIVATE);
        privateUserPage.click(TestConfig.CONNECT_BUTTON);

        privateUserPage.fill(TestConfig.MESSAGE_INPUT, TestConfig.Messages.PRIVATE_MSG);
        privateUserPage.click(TestConfig.SEND_BUTTON);

        PlaywrightAssertions.assertThat(page.locator(TestConfig.MESSAGES_AREA))
                .not().containsText(TestConfig.Messages.PUBLIC_MSG);
    }


    @Test
    @DisplayName("Блокировка повторного подключения с тем же логином")
    void testMultipleConnections() {
        page.fill(TestConfig.USERNAME_FIELD, TestConfig.Users.MULTI_USER);
        page.selectOption(TestConfig.ROOM_SELECTOR, TestConfig.Rooms.PUBLIC);
        page.click(TestConfig.CONNECT_BUTTON);

        Page secondTab = context.newPage();
        secondTab.navigate(TestConfig.getPageUrl(server.getPort()));
        secondTab.fill(TestConfig.USERNAME_FIELD, TestConfig.Users.MULTI_USER);
        secondTab.selectOption(TestConfig.ROOM_SELECTOR, TestConfig.Rooms.PUBLIC);
        secondTab.click(TestConfig.CONNECT_BUTTON);

        PlaywrightAssertions.assertThat(secondTab.locator(TestConfig.MESSAGES_AREA))
                .containsText(TestConfig.Messages.ERROR_LOGIN_EXISTS);
    }


    @Test
    @DisplayName("Автоматическое восстановление соединения после разрыва")
    void testConnectionLossRecovery() {
        page.fill(TestConfig.USERNAME_FIELD, TestConfig.Users.RESILIENT_USER);
        page.selectOption(TestConfig.ROOM_SELECTOR, TestConfig.Rooms.PUBLIC);
        page.click(TestConfig.CONNECT_BUTTON);

        page.evaluate(TestConfig.Connection.WS_CLOSE_SCRIPT);
        page.click(TestConfig.CONNECT_BUTTON);

        PlaywrightAssertions.assertThat(page.locator(TestConfig.MESSAGES_AREA))
                .containsText(TestConfig.Users.RESILIENT_USER +
                        TestConfig.Messages.CONNECTION_SUCCESS);
    }
}