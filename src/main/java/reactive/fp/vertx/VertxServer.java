package reactive.fp.vertx;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocketFrame;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import reactive.fp.mappers.Mappers;
import reactive.fp.commands.CommandRegistry;
import reactive.fp.config.WebServerConfig;
import reactive.fp.types.Command;
import reactive.fp.types.Event;
import reactive.fp.types.WebServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import static reactive.fp.utils.WebUtils.includeEndDelimiter;
import static reactive.fp.utils.WebUtils.includeStartDelimiter;

/**
 * @author OZY on 2015.11.23.
 */
public class VertxServer implements WebServer {

    private final WebServerConfig config;
    private final CommandRegistry commands;
    private final Vertx vertx;
    private final HttpServer httpServer;

    public VertxServer(WebServerConfig config, CommandRegistry commands) {
        this.config = config;
        this.commands = commands;
        this.vertx = Vertx.vertx();
        this.httpServer = vertx.createHttpServer(new HttpServerOptions()
                .setPort(config.port)
                .setSsl(false)
                .setReuseAddress(true));
    }

    @Override
    public void start() {
        httpServer.listen(config.port);
    }

    @Override
    public void stop() {
        httpServer.close();
    }

    @Override
    public void setupRoutes() {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        httpServer.websocketHandler(webSocketHandler());
        httpServer.requestHandler(router::accept);
    }

    protected Handler<ServerWebSocket> webSocketHandler() {
        return wsSocket -> commands.findCommand(wsSocket.path()).ifPresent(f -> wsSocket.handler(buffer -> {
            try {
                Command<?> receivedCommand = Mappers.jsonMapper.readValue(buffer.getBytes(), Command.class);
                Optional.ofNullable(f)
                        .map(command -> command.apply(receivedCommand.payload))
                        .ifPresent(observable -> observable
                                .subscribe(
                                        payload -> send(wsSocket, Event.onNext(payload)),
                                        throwable -> send(wsSocket, Event.onError(throwable)),
                                        () -> send(wsSocket, Event.onCompleted("Completed")))
                        );
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }));
    }

    public void send(ServerWebSocket ws, Event<?> event) {
        try {
            final String value = Mappers.jsonMapper.writeValueAsString(event);
            ws.writeFrame(WebSocketFrame.textFrame(value, false));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected String root() {
        return includeEndDelimiter(includeStartDelimiter(config.root));
    }
    }
