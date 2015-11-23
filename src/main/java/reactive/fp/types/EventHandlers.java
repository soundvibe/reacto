package reactive.fp.types;

import java.util.Optional;

/**
 * @author OZY on 2015.11.13.
 */
public class EventHandlers<T,U> {

    public final EventHandler<T,U> mainNodeClient;
    public final Optional<EventHandler<T,U>> fallbackNodeClient;

    public EventHandlers(EventHandler<T,U> mainNodeClient, Optional<EventHandler<T,U>> fallbackNodeClient) {
        this.mainNodeClient = mainNodeClient;
        this.fallbackNodeClient = fallbackNodeClient;
    }

    public EventHandlers<T,U> copy(EventHandler<T,U> fallbackNodeClient) {
        return new EventHandlers<T,U>(mainNodeClient, Optional.ofNullable(fallbackNodeClient));
    }

    @Override
    public String toString() {
        return "EventHandlers{" +
                "mainNodeClient=" + mainNodeClient +
                ", fallbackNodeClient=" + fallbackNodeClient +
                '}';
    }
}
