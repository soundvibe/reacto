package net.soundvibe.reacto;

import com.netflix.hystrix.*;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.ext.web.Router;
import net.soundvibe.reacto.server.*;
import net.soundvibe.reacto.server.handlers.*;
import net.soundvibe.reacto.utils.Factories;
import org.junit.*;
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
        Vertx vertx = Factories.vertx();
        final Router router = Router.router(vertx);
        router.route("/test/hystrix.stream")
                .handler(new SSEHandler(HystrixEventStreamHandler::handle));
        vertxServer = new VertxServer(new ServiceOptions("test", "test"), router, vertx.createHttpServer(new HttpServerOptions().setPort(8282)),
               CommandRegistry.of("bla", o -> Observable.empty()));
        vertxServer.start().toBlocking().subscribe();
        lastData = new AtomicReference<>();
        httpClient = vertx.createHttpClient();
    }

    @After
    public void tearDown() throws Exception {
        httpClient.close();
        vertxServer.stop().toBlocking().subscribe();
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
