package net.soundvibe.reacto.client.events;

import java.util.Optional;

/**
 * @author OZY on 2015.11.13.
 */
public class EventHandlers {

    public final EventHandler mainNodeClient;
    public final Optional<EventHandler> fallbackNodeClient;

    public EventHandlers(EventHandler mainNodeClient, Optional<EventHandler> fallbackNodeClient) {
        this.mainNodeClient = mainNodeClient;
        this.fallbackNodeClient = fallbackNodeClient;
    }

    public EventHandlers copy(EventHandler fallbackNodeClient) {
        return new EventHandlers(mainNodeClient, Optional.ofNullable(fallbackNodeClient));
    }
}
