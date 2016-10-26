package net.soundvibe.reacto.client.commands;

import net.soundvibe.reacto.client.events.EventHandler;
import net.soundvibe.reacto.discovery.DiscoverableService;
import net.soundvibe.reacto.types.*;
import rx.Observable;

/**
 * @author OZY on 2016.09.06.
 */
public final class VertxDiscoverableCommandExecutor implements CommandExecutor {

    private final DiscoverableService discoverableService;
    private final EventHandler eventHandler;

    public VertxDiscoverableCommandExecutor(DiscoverableService discoverableService, EventHandler eventHandler) {
        this.discoverableService = discoverableService;
        this.eventHandler = eventHandler;
    }

    @Override
    public Observable<Event> execute(Command command) {
        return eventHandler.toObservable(command)
            .onExceptionResumeNext(retry(command));
    }

    private Observable<Event> retry(Command command) {
        return Observable.empty();
    }
}
