package net.soundvibe.reacto;

import com.netflix.hystrix.*;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.servicediscovery.ServiceDiscovery;
import net.soundvibe.reacto.discovery.ReactoServiceRegistry;
import net.soundvibe.reacto.mappers.jackson.JacksonMapper;
import net.soundvibe.reacto.server.*;
import net.soundvibe.reacto.types.*;
import org.junit.*;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertTrue;

/**
 * @author Cipolinas on 2015.11.23.
 */
public class HystrixEventStreamHandlerTest {

    private static final int PORT = 8282;
    private VertxServer vertxServer;
    private ReactoServiceRegistry serviceRegistry;
    private final Vertx vertx = Vertx.vertx();

    @Before
    public void setUp() throws Exception {
        final Router router = Router.router(vertx);
        serviceRegistry = new ReactoServiceRegistry(
                ServiceDiscovery.create(vertx),
                new JacksonMapper(Json.mapper));
        vertxServer = new VertxServer(new ServiceOptions("test", "test"),
                router, vertx.createHttpServer(new HttpServerOptions().setPort(PORT)),
                CommandRegistry.of("demo", o -> Observable.just(Event.create("foo"), Event.create("bar"))),
                serviceRegistry);
        vertxServer.start().toBlocking().subscribe();
    }

    @After
    public void tearDown() throws Exception {
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

        final String actual = getBodyAsString("/test/hystrix.stream");
        assertTrue("Should start with data: ", actual.startsWith("data: {"));
    }

    @Test
    public void shouldExecuteCommandAndPushEventStream() throws Exception {
        TestSubscriber<Event> testSubscriber = new TestSubscriber<>();
        serviceRegistry.execute(Command.create("demo"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValueCount(2);
        final String actual = getBodyAsString("/test/reacto.command.stream");
        assertTrue("Should start with data: ", actual.startsWith("data: {"));
    }

    private String getBodyAsString(String relativeUrl) {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicReference<String> bodyAsString = new AtomicReference<>();
        final HttpClient httpClient = vertx.createHttpClient();
        httpClient.getNow(PORT, "localhost", relativeUrl,
                httpClientResponse -> httpClientResponse.handler(buffer -> {
                    final byte[] bytes = buffer.getBytes();
                    String data = new String(bytes);
                    bodyAsString.set(data);
                    countDownLatch.countDown();
                }));
        try {
            countDownLatch.await(1000L, TimeUnit.MILLISECONDS);
            return bodyAsString.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            httpClient.close();
        }
    }

    private class FooCommand extends HystrixObservableCommand<String> {
        private final String value;

        FooCommand(String value) {
            super(HystrixCommandGroupKey.Factory.asKey("foo"));
            this.value = value;
        }

        @Override
        protected Observable<String> construct() {
            return Observable.just(value);
        }
    }
}
