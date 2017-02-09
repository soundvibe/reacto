package net.soundvibe.reacto.discovery;

import net.soundvibe.reacto.client.commands.*;
import net.soundvibe.reacto.client.events.EventHandler;
import net.soundvibe.reacto.types.*;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

/**
 * @author OZY on 2017.01.24.
 */
public class ServiceExecutorTest {

    @Test
    public void shouldExecute() throws Exception {
        ServiceExecutor sut = getRegistry();

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

    private ServiceExecutor getRegistry() {
        return new ServiceExecutor() {
            @Override
            public <E, C> Observable<E> execute(C command, Class<? extends E> eventClass, LoadBalancer<EventHandler> loadBalancer, CommandExecutorFactory commandExecutorFactory) {
                return Observable.just(eventClass.cast(Event.create("bar")));
            }
        };
    }
}