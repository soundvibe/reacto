package net.soundvibe.reacto.discovery;

import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;
import net.soundvibe.reacto.client.commands.*;
import net.soundvibe.reacto.client.events.CommandHandler;
import net.soundvibe.reacto.types.*;
import org.junit.Test;

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
        testSubscriber.assertComplete();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(Event.create("bar"));

        final TestSubscriber<Event> testSubscriber2 = new TestSubscriber<>();
        sut.execute(Command.create("bar"), Event.class, CommandExecutors.reacto())
                .subscribe(testSubscriber2);

        testSubscriber2.awaitTerminalEvent();
        testSubscriber2.assertComplete();
        testSubscriber2.assertNoErrors();
        testSubscriber2.assertValue(Event.create("bar"));
    }

    private ServiceExecutor getRegistry() {
        return new ServiceExecutor() {
            @Override
            public <E, C> Flowable<E> execute(C command, Class<? extends E> eventClass, LoadBalancer<CommandHandler> loadBalancer, CommandExecutorFactory commandExecutorFactory) {
                return Flowable.just(eventClass.cast(Event.create("bar")));
            }
        };
    }
}