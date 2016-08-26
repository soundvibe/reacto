package net.soundvibe.reacto.client.events;

import io.vertx.core.http.WebSocketStream;
import net.soundvibe.reacto.types.Command;
import net.soundvibe.reacto.types.Event;
import rx.Observable;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * @author OZY on 2015.11.23.
 */
public class VertxDiscoverableEventHandler implements EventHandler {

    private final WebSocketStream webSocketStream;
    private final BiFunction<WebSocketStream, Command, Observable<Event>> eventHandler;

    public VertxDiscoverableEventHandler(WebSocketStream webSocketStream,
                                         BiFunction<WebSocketStream, Command, Observable<Event>> eventHandler) {
        Objects.requireNonNull(webSocketStream, "Observable cannot be null");
        Objects.requireNonNull(eventHandler, "eventHandler cannot be null");
        this.webSocketStream = webSocketStream;
        this.eventHandler = eventHandler;
    }

    @Override
    public Observable<Event> toObservable(Command command) {
        return eventHandler.apply(webSocketStream, command);
    }
}
