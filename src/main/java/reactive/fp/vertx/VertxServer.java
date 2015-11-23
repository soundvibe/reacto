package reactive.fp.vertx;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocketFrame;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import reactive.fp.commands.CommandRegistry;
import reactive.fp.config.WebServerConfig;
import reactive.fp.mappers.Mappers;
import reactive.fp.types.Command;
import reactive.fp.types.Event;
import reactive.fp.types.WebServer;

import java.io.IOException;
import java.io.UncheckedIOException;

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
        setupRoutes();
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
        return wsSocket ->
                commands.findCommand(getCommandNameFrom(wsSocket.path())).ifPresent(command -> wsSocket.handler(buffer -> {
            try {
                Command<?> receivedArgument = Mappers.jsonMapper.readValue(buffer.getBytes(), Command.class);
                command.apply(receivedArgument.payload)
                                .subscribe(
                                        payload -> send(wsSocket, Event.onNext(payload)),
                                        throwable -> send(wsSocket, Event.onError(throwable)),
                                        () -> send(wsSocket, Event.onCompleted("Completed")))
                        ;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }));
    }

    public void send(ServerWebSocket ws, Event<?> event) {
        try {
            final String value = Mappers.jsonMapper.writeValueAsString(event);
            ws.writeFrame(WebSocketFrame.textFrame(value, true));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected String getCommandNameFrom(String path) {
        return path.substring(path.lastIndexOf("/") + 1, path.length());
    }

    protected String root() {
        return includeEndDelimiter(includeStartDelimiter(config.root));
    }
    }
