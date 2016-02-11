package io.reacto.client.commands.hystrix;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.junit.Test;
import io.reacto.client.commands.CommandExecutor;
import io.reacto.client.commands.CommandExecutors;
import io.reacto.types.Command;
import io.reacto.types.Event;
import io.reacto.types.Pair;
import rx.Observable;
import rx.observers.TestSubscriber;

/**
 * @author Cipolinas on 2015.12.01.
 */
public class HystrixInMemoryCommandExecutorTest {

    @Test
    public void shouldExecuteCommand() throws Exception {
        final Event event = Event.create("foo", Pair.of("foo", "bar"));
        TestSubscriber<Event> testSubscriber = new TestSubscriber<>();
        CommandExecutor sut = CommandExecutors.inMemory(o -> event.toObservable());

        sut.execute(Command.create("foo"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(event);
    }

    @Test
    public void shouldGetError() throws Exception {
        TestSubscriber<Event> testSubscriber = new TestSubscriber<>();
        CommandExecutor sut = CommandExecutors.inMemory(o -> Observable.error(new IllegalArgumentException("error")));

        sut.execute(Command.create("foo", Pair.of("foo", "bar")))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNotCompleted();
        testSubscriber.assertNoValues();
        testSubscriber.assertError(HystrixRuntimeException.class);
    }
}
