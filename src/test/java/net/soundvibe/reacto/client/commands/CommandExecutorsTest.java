package net.soundvibe.reacto.client.commands;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import net.soundvibe.reacto.types.*;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

/**
 * @author Cipolinas on 2016.02.18.
 */
public class CommandExecutorsTest {

    private final TestSubscriber<Event> testSubscriber = new TestSubscriber<>();

    @Test
    public void shouldCreateWebSocketExecutor() throws Exception {
        final CommandExecutor sut = CommandExecutors.webSocket(Nodes.of("http://dummy/test"), 1000);
        sut.execute(Command.create("foo"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertError(HystrixRuntimeException.class);
    }

    @Test
    public void shouldCreateInMemoryExecutor() throws Exception {
        final CommandExecutor sut = CommandExecutors.inMemory(command -> Observable.empty(), 1000);
        sut.execute(Command.create("foo"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertNoValues();
    }

    @Test
    public void shouldCallFallbackWhenInMemoryMainExecutorFails() throws Exception {
        final CommandExecutor sut = CommandExecutors.inMemoryWithFallback(
                main -> Observable.error(new IllegalStateException("error")),
                fallback -> Observable.just(Event.create("ok")));
        sut.execute(Command.create("ok"))
                .subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(Event.create("ok"));
    }

}
