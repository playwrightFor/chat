package server;

/**
 * Запускает сервер WebSocket.
 * Порт, на котором будет слушать сервер.
 */
public class Start {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 1401;
        new WebSocketServer(port).start();
    }
}
