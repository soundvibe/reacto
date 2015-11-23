package reactive.fp.types;

import java.util.Optional;

/**
 * @author OZY on 2015.11.13.
 */
public class EventHandlers<T> {

    public final EventHandler<T> mainNodeClient;
    public final Optional<EventHandler<T>> fallbackNodeClient;

    public EventHandlers(EventHandler<T> mainNodeClient, Optional<EventHandler<T>> fallbackNodeClient) {
        this.mainNodeClient = mainNodeClient;
        this.fallbackNodeClient = fallbackNodeClient;
    }

    public EventHandlers<T> copy(EventHandler<T> fallbackNodeClient) {
        return new EventHandlers<T>(mainNodeClient, Optional.ofNullable(fallbackNodeClient));
    }

    @Override
    public String toString() {
        return "EventHandlers{" +
                "mainNodeClient=" + mainNodeClient +
                ", fallbackNodeClient=" + fallbackNodeClient +
                '}';
    }
}
