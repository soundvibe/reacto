package net.soundvibe.reacto.client.commands.hystrix;

import net.soundvibe.reacto.client.commands.CommandExecutors;
import net.soundvibe.reacto.client.errors.CannotConnectToWebSocket;
import net.soundvibe.reacto.types.Command;
import net.soundvibe.reacto.types.Event;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Optional;

/**
 * @author Cipolinas on 2016.02.18.
 */
public class HystrixCommandExecutorTest {

    private final TestSubscriber<Event> testSubscriber = new TestSubscriber<>();

    @Test
    public void shouldGetErrorWhenEventHandlersAreEmpty() throws Exception {
        HystrixCommandExecutor sut = new HystrixCommandExecutor(Optional::empty, CommandExecutors.defaultHystrixSetter());
        sut.execute(Command.create("foo"))
            .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertError(CannotConnectToWebSocket.class);
    }
}
