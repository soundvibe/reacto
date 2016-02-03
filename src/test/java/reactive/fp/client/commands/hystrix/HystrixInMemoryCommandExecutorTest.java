package reactive.fp.client.commands.hystrix;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.junit.Test;
import reactive.fp.client.commands.CommandExecutor;
import reactive.fp.client.commands.CommandExecutors;
import rx.Observable;
import rx.observers.TestSubscriber;

/**
 * @author Cipolinas on 2015.12.01.
 */
public class HystrixInMemoryCommandExecutorTest {

    @Test
    public void shouldExecuteCommand() throws Exception {
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        CommandExecutor<String, String> sut = CommandExecutors.inMemory("foo", o -> Observable.just((String) o));

        sut.execute("bar")
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue("bar");
    }

    @Test
    public void shouldGetError() throws Exception {
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        CommandExecutor<String, String> sut = CommandExecutors.inMemory("foo", o -> Observable.error(new IllegalArgumentException("error")));

        sut.execute("bar")
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNotCompleted();
        testSubscriber.assertNoValues();
        testSubscriber.assertError(HystrixRuntimeException.class);
    }
}
