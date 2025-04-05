package health;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Класс HealthCheckServlet представляет собой сервлет для проверки состояния сервера.
 * Этот сервлет обрабатывает HTTP GET запросы по пути "/health" и возвращает
 * информацию о состоянии сервера в формате JSON.
 * При успешном выполнении запроса сервлет отправляет клиенту ответ с кодом 200 (OK) и
 * сообщением о том, что сервер работает корректно.
 */
@WebServlet("/health")
public class HealthCheckServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        res.setStatus(HttpServletResponse.SC_OK);

        String jsonResponse = "{\"status\": \"UP\", \"message\": \"Сервер работает корректно.\"}";

        PrintWriter out = res.getWriter();
        out.print(jsonResponse);
        out.flush();
    }
}
