package net.soundvibe.reacto.discovery;

import net.soundvibe.reacto.client.commands.*;
import net.soundvibe.reacto.client.events.EventHandler;
import net.soundvibe.reacto.discovery.types.ServiceRecord;
import net.soundvibe.reacto.types.*;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

/**
 * @author OZY on 2017.01.24.
 */
public class ServiceRegistryTest {

    @Test
    public void shouldExecute() throws Exception {
        ServiceRegistry sut = getRegistry();

        final TestSubscriber<Event> testSubscriber = new TestSubscriber<>();
        sut.execute(Command.create("foo"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(Event.create("bar"));

        final TestSubscriber<Event> testSubscriber2 = new TestSubscriber<>();
        sut.execute(Command.create("bar"), Event.class, CommandExecutors.reacto())
                .subscribe(testSubscriber2);

        testSubscriber2.awaitTerminalEvent();
        testSubscriber2.assertCompleted();
        testSubscriber2.assertNoErrors();
        testSubscriber2.assertValue(Event.create("bar"));
    }

    private ServiceRegistry getRegistry() {
        return new ServiceRegistry() {
            @Override
            public <E, C> Observable<E> execute(C command, Class<? extends E> eventClass, LoadBalancer<EventHandler> loadBalancer, CommandExecutorFactory commandExecutorFactory) {
                return Observable.just(eventClass.cast(Event.create("bar")));
            }

            @Override
            public Observable<Any> unpublish(ServiceRecord serviceRecord) {
                return Observable.just(Any.VOID);
            }
        };
    }
}