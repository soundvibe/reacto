package net.soundvibe.reacto.client.commands.hystrix;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import net.soundvibe.reacto.types.Command;
import net.soundvibe.reacto.types.Event;
import org.junit.Test;
import net.soundvibe.reacto.client.commands.CommandExecutor;
import net.soundvibe.reacto.client.commands.CommandExecutors;
import net.soundvibe.reacto.types.Pair;
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
