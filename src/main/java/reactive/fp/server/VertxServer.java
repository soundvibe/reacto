package reactive.fp.server;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocketFrame;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import reactive.fp.types.Command;
import reactive.fp.types.Event;
import reactive.fp.utils.Factories;

import static reactive.fp.mappers.Mappers.fromJsonToCommand;
import static reactive.fp.mappers.Mappers.messageToJsonBytes;
import static reactive.fp.utils.WebUtils.includeEndDelimiter;
import static reactive.fp.utils.WebUtils.includeStartDelimiter;

/**
 * @author OZY on 2015.11.23.
 */
public class VertxServer implements Server {

    private final WebServerConfig config;
    private final CommandRegistry commands;
    private final Vertx vertx;
    private final HttpServer httpServer;

    public VertxServer(WebServerConfig config, CommandRegistry commands) {
        this.config = config;
        this.commands = commands;
        this.vertx = Factories.vertx();
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

    private void setupRoutes() {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        httpServer.websocketHandler(webSocketHandler());
        router.route(root() + "hystrix.stream")
                .handler(new HystrixEventStreamHandler());
        httpServer.requestHandler(router::accept);
    }

    private Handler<ServerWebSocket> webSocketHandler() {
        return wsSocket ->
                commands.findCommand(getCommandNameFrom(wsSocket.path())).ifPresent(command -> wsSocket.handler(buffer -> {
                    Command<?> receivedArgument = fromJsonToCommand(buffer.getBytes());
                    command.apply(receivedArgument.payload)
                            .subscribe(
                                    payload -> send(wsSocket, Event.onNext(payload)),
                                    throwable -> send(wsSocket, Event.onError(throwable)),
                                    () -> send(wsSocket, Event.onCompleted("Completed")))
                    ;
                }));
    }

    private void send(ServerWebSocket ws, Event<?> event) {
        final byte[] bytes = messageToJsonBytes(event);
        ws.writeFrame(WebSocketFrame.binaryFrame(Buffer.buffer(bytes), true));
    }

    private String getCommandNameFrom(String path) {
        return path.substring(path.lastIndexOf("/") + 1, path.length());
    }


    private String root() {
        return includeEndDelimiter(includeStartDelimiter(config.root));
    }
    }
