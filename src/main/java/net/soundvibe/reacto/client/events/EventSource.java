package net.soundvibe.reacto.client.events;

import java.util.function.Consumer;

/**
 * @author OZY on 2017.01.13.
 */
public interface EventSource {

    void open();

    EventSource onOpen(Runnable openConsumer);

    EventSource onError(Consumer<Throwable> errorConsumer);

    EventSource onMessage(Consumer<String> messageConsumer);

}
