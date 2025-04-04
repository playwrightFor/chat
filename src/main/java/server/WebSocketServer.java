package server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import java.net.URISyntaxException;

/**
 * Класс WebSocketServer отвечает за создание и запуск WebSocket-сервера.
 * Сервер принимает подключения клиентов и обрабатывает WebSocket-соединения.
 */
public class WebSocketServer {
    private final Server server;

    public WebSocketServer(int port) throws URISyntaxException {
        this.server = new Server(port);

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setResourceBase(
                getClass().getClassLoader().getResource("static").toURI().toString()
        );

        ServletContextHandler wsHandler = new ServletContextHandler();
        wsHandler.setContextPath("/");
        wsHandler.addServlet(new ServletHolder(new WebSocketServlet() {
            @Override
            public void configure(WebSocketServletFactory factory) {
                factory.register(WebSocketHandler.class);
            }
        }), "/chat");

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{resourceHandler, wsHandler});
        server.setHandler(handlers);
    }

    public void start() throws Exception {
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }
}