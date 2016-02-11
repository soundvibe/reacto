package io.reacto.server;

import io.reacto.server.handlers.SSEHandler;
import io.reacto.server.handlers.WebSocketCommandHandler;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.reacto.server.handlers.CommandHandler;
import io.reacto.server.handlers.HystrixEventStreamHandler;

import java.util.Objects;

import static io.reacto.utils.WebUtils.includeEndDelimiter;
import static io.reacto.utils.WebUtils.includeStartDelimiter;

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
        return includeEndDelimiter(includeStartDelimiter(root));
    }
}
