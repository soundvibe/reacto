package reactive.fp.types;

import rx.Observable;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Cipolinas on 2015.11.16.
 */
public interface EventHandler<T,U> {

    void start();

    void close();

    Observable<U> toObservable(String commandName, T arg);

    static <T,U> Optional<EventHandler<T,U>> canStartEventHandler(URI url, Function<URI,EventHandler<T,U>> factory) {
        EventHandler<T,U> eventHandler = factory.apply(url);
        try {
            eventHandler.start();
            return Optional.of(eventHandler);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
