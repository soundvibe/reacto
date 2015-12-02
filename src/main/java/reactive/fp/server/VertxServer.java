package reactive.fp.server;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocketFrame;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import reactive.fp.types.Command;
import reactive.fp.types.Event;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.util.Objects;

import static reactive.fp.mappers.Mappers.fromJsonToCommand;
import static reactive.fp.mappers.Mappers.messageToJsonBytes;
import static reactive.fp.utils.WebUtils.includeEndDelimiter;
import static reactive.fp.utils.WebUtils.includeStartDelimiter;

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
        router.route().handler(BodyHandler.create());
        httpServer.websocketHandler(webSocketHandler());
        router.route(root() + "hystrix.stream")
                .handler(new HystrixEventStreamHandler());
        httpServer.requestHandler(router::accept);
    }

    private Handler<ServerWebSocket> webSocketHandler() {
        return wsSocket ->
                commands.findCommand(getCommandNameFrom(wsSocket.path())).ifPresent(command -> wsSocket.handler(buffer -> {
                    try {
                        Command<?> receivedArgument = fromJsonToCommand(buffer.getBytes());
                        final Subscription subscription = command.apply(receivedArgument.payload)
                                .subscribeOn(Schedulers.computation())
                                .subscribe(
                                        payload -> send(wsSocket, Event.onNext(payload)),
                                        throwable -> send(wsSocket, Event.onError(throwable)),
                                        () -> send(wsSocket, Event.onCompleted("Completed")));
                        wsSocket.closeHandler(event -> subscription.unsubscribe());
                    } catch (Throwable e) {
                        send(wsSocket, Event.onError(e));
                    }
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
        return includeEndDelimiter(includeStartDelimiter(root));
    }
    }
