package net.soundvibe.reacto.server;

import net.soundvibe.reacto.server.handlers.SSEHandler;
import net.soundvibe.reacto.server.handlers.WebSocketCommandHandler;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import net.soundvibe.reacto.server.handlers.CommandHandler;
import net.soundvibe.reacto.server.handlers.HystrixEventStreamHandler;
import net.soundvibe.reacto.utils.WebUtils;

import java.util.Objects;

/**
 * @author OZY on 2015.11.23.
 */
public class VertxServer implements Server {

    private final String root;
    private final CommandRegistry commands;
    private final HttpServer httpServer;
    private final Router router;

    public VertxServer(Router router, HttpServer httpServer, String root, CommandRegistry commands) {
        Objects.requireNonNull(router, "Router cannot be null");
        Objects.requireNonNull(httpServer, "HttpServer cannot be null");
        Objects.requireNonNull(root, "Root cannot be null");
        Objects.requireNonNull(commands, "CommandRegistry cannot be null");
        this.router = router;
        this.httpServer = httpServer;
        this.root = root;
        this.commands = commands;
    }

    @Override
    public void start() {
        setupRoutes();
        httpServer.listen();
    }

    @Override
    public void stop() {
        httpServer.close();
    }

    private void setupRoutes() {
        httpServer.websocketHandler(new WebSocketCommandHandler(new CommandHandler(commands)));
        router.route(root() + "hystrix.stream")
                .handler(new SSEHandler(HystrixEventStreamHandler::handle));
        httpServer.requestHandler(router::accept);
    }

    private String root() {
        return WebUtils.includeEndDelimiter(WebUtils.includeStartDelimiter(root));
    }
}
