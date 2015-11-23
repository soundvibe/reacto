package reactive.fp.vertx;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import io.vertx.core.http.HttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import reactive.fp.server.CommandRegistry;
import reactive.fp.server.VertxServer;
import reactive.fp.server.WebServerConfig;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertTrue;

/**
 * @author Cipolinas on 2015.11.23.
 */
public class HystrixEventStreamHandlerTest {

    private VertxServer vertxServer;
    private HttpClient httpClient;
    private AtomicReference<String> lastData;

    @Before
    public void setUp() throws Exception {
        vertxServer = new VertxServer(new WebServerConfig(8282, "test"), new CommandRegistry());
        vertxServer.start();
        lastData = new AtomicReference<>();
        httpClient = vertxServer.vertx.createHttpClient();
    }

    @After
    public void tearDown() throws Exception {
        httpClient.close();
        vertxServer.stop();
    }

    @Test
    public void shouldWriteSomeDataWhenCommandIsExecuted() throws Exception {
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        new FooCommand("foo").toObservable()
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue("foo");

        //hystrix stream address
        httpClient.getNow(8282, "localhost","/test/hystrix.stream", httpClientResponse -> httpClientResponse.handler(buffer -> {
            final byte[] bytes = buffer.getBytes();
            String data = new String(bytes);
            if (lastData.get() == null) {
                lastData.set(data);
            }
        }));
        Thread.sleep(1000L);
        final String actual = lastData.get();
        assertTrue("Should start with data: ", actual.startsWith("data: {"));
    }

    private class FooCommand extends HystrixObservableCommand<String> {
        private final String value;

        public FooCommand(String value) {
            super(HystrixCommandGroupKey.Factory.asKey("foo"));
            this.value = value;
        }

        @Override
        protected Observable<String> construct() {
            return Observable.just(value);
        }
    }
}
