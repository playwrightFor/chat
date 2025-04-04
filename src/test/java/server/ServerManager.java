package server;

import lombok.Getter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Утилитарный (кастомный) класс для управления жизненным циклом сервера
 * в тестовом окружении, предоставляя удобный API для:
 * - Запуска сервера на заданном порту
 * - Остановки сервера
 * - Обработки ошибок.
 */
public class ServerManager {
    @Getter
    private final int port;
    private WebSocketServer webSocketServer;

    public ServerManager() {
        this.port = findFreePort();
    }

    private int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось найти свободный порт", e);
        }
    }

    public void start() {
        try {
            webSocketServer = new WebSocketServer(port);
            new Thread(() -> {
                try {
                    webSocketServer.start();
                } catch (Exception e) {
                    throw new RuntimeException("Server start failed", e);
                }
            }).start();
            waitUntilReady();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка запуска сервера", e);
        }
    }

    private void waitUntilReady() throws InterruptedException {
        int timeout = 15000;
        int interval = 500;
        int elapsed = 0;

        while (!isPortOpen()) {
            if (elapsed >= timeout) throw new RuntimeException("Timeout");
            Thread.sleep(interval);
            elapsed += interval;
        }
    }

    private boolean isPortOpen() {
        try (Socket ignored = new Socket("localhost", port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void stop() {
        try {
            if (webSocketServer != null) {
                webSocketServer.stop();
            }
        } catch (Exception e) {
            System.err.println("Ошибка остановки сервера: " + e.getMessage());
        }
    }
}