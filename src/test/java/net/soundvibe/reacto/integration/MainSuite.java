package net.soundvibe.reacto.integration;

import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.json.*;
import io.vertx.ext.web.Router;
import io.vertx.servicediscovery.ServiceDiscovery;
import net.soundvibe.reacto.client.commands.CommandExecutors;
import net.soundvibe.reacto.errors.*;
import net.soundvibe.reacto.client.events.EventHandlerRegistry;
import net.soundvibe.reacto.client.events.vertx.VertxDiscoverableEventHandler;
import net.soundvibe.reacto.discovery.types.ServiceType;
import net.soundvibe.reacto.discovery.vertx.VertxServiceRegistry;
import net.soundvibe.reacto.mappers.jackson.*;
import net.soundvibe.reacto.metric.*;
import net.soundvibe.reacto.server.*;
import net.soundvibe.reacto.server.vertx.VertxServer;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.utils.*;
import net.soundvibe.reacto.utils.models.*;
import org.junit.*;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/**
 * @author Cipolinas on 2015.12.01.
 */
public class MainSuite {

    private static final String TEST_COMMAND = "test";
    private static final String TEST_COMMAND_MANY = "testMany";
    private static final String TEST_FAIL_COMMAND = "testFail";
    private static final String TEST_FAIL_BUT_FALLBACK_COMMAND = "testFailFallback";
    private static final String LONG_TASK = "longTask";
    private static final String COMMAND_WITHOUT_ARGS = "argLessCommand";
    private static final String COMMAND_CUSTOM_ERROR = "commandCustomError";
    private static final String COMMAND_EMIT_AND_FAIL = "emitAndFail";

    private static final int MAIN_SERVER_PORT = 8282;
    private static final int FALLBACK_SERVER_PORT = 8383;

    private static VertxServer vertxServer;
    private static VertxServer fallbackVertxServer;
    private static ServiceDiscovery serviceDiscovery;
    private static VertxServiceRegistry registry;
    private static VertxServiceRegistry registryTyped;

    private final TestSubscriber<Event> testSubscriber = new TestSubscriber<>();
    private static Vertx vertx;
    private final TestSubscriber<DemoMade> typedSubscriber = new TestSubscriber<>();

    @BeforeClass
    public static void setUpClass() throws Exception {
        vertx = Vertx.vertx();
        serviceDiscovery = ServiceDiscovery.create(vertx);

        final EventHandlerRegistry eventHandlerRegistry = EventHandlerRegistry.Builder.create()
                .register(ServiceType.WEBSOCKET, serviceRecord -> VertxDiscoverableEventHandler.create(serviceRecord, serviceDiscovery))
                .build();

        registry = new VertxServiceRegistry(eventHandlerRegistry, serviceDiscovery, new DemoServiceRegistryMapper());
        registryTyped = new VertxServiceRegistry(eventHandlerRegistry, serviceDiscovery, new JacksonMapper(Json.mapper));

        final HttpServer mainHttpServer = vertx.createHttpServer(new HttpServerOptions()
                .setPort(MAIN_SERVER_PORT)
                .setSsl(false)
                .setReuseAddress(true));

        HttpServer fallbackHttpServer = vertx.createHttpServer(new HttpServerOptions()
                .setPort(FALLBACK_SERVER_PORT)
                .setSsl(false)
                .setReuseAddress(true));

        final Router router = Router.router(vertx);
        router.route("/health").handler(event -> event.response().end("ok"));
        vertxServer = new VertxServer(new ServiceOptions("dist", "dist/", "0.1")
                , router, mainHttpServer, createMainCommands(), registry);
        fallbackVertxServer = new VertxServer(new ServiceOptions("dist","dist/", "0.1")
                , Router.router(vertx), fallbackHttpServer,  createFallbackCommands(), registryTyped);
        fallbackVertxServer.start().toBlocking().subscribe();
        vertxServer.start().toBlocking().subscribe();
    }

    @Before
    public void setUp() throws Exception {
        registry.publish(registry.getRecord()).toBlocking().subscribe(record -> {}, Throwable::printStackTrace);
        registryTyped.publish(registryTyped.getRecord()).toBlocking().subscribe(record -> {}, Throwable::printStackTrace);
    }

    private static CommandRegistry createFallbackCommands() {
        return CommandRegistry.ofTyped(
                    Feed.class, Animal.class,
                    feed -> Observable.just(
                            new Dog("Dog ate " + feed.meal),
                            new Cat("Cat ate " + feed.meal)
                    ),
                    new JacksonMapper(Json.mapper))
                .and(JacksonCommand.class, JacksonEvent.class, jacksonCommand -> Observable.error(new RuntimeException("test error")))
                .and(TEST_FAIL_BUT_FALLBACK_COMMAND,
                o -> event1Arg("Recovered: " + o.get("arg")).toObservable());
    }

    private static CommandRegistry createMainCommands() {
        return CommandRegistry.ofTyped(MakeDemo.class, DemoMade.class,
                    makeDemo -> Observable.just(new DemoMade(makeDemo.name)),
                    new DemoCommandRegistryMapper())
                .and(TEST_COMMAND, cmd ->
                    event1Arg("Called command with arg: " + cmd.get("arg")).toObservable()
                )
                .and(TEST_COMMAND_MANY, o -> Observable.just(
                        event1Arg("1. Called command with arg: " + o.get("arg")),
                        event1Arg("2. Called command with arg: " + o.get("arg")),
                        event1Arg("3. Called command with arg: " + o.get("arg"))
                ))
                .and(TEST_FAIL_COMMAND, o -> Observable.error(new RuntimeException("failed")))
                .and(TEST_FAIL_BUT_FALLBACK_COMMAND, o -> Observable.error(new RuntimeException("failed")))
                .and(COMMAND_WITHOUT_ARGS, o -> event1Arg("ok").toObservable())
                .and(COMMAND_CUSTOM_ERROR, o -> Observable.error(new CustomError(o.get("arg"))))
                .and(COMMAND_EMIT_AND_FAIL, command -> Observable.create(subscriber -> {
                    subscriber.onNext(Event.create("ok"));
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }))
                .and(LONG_TASK, interval -> Observable.create(subscriber -> {
                    try {
                        Thread.sleep(Integer.valueOf(interval.get("arg")));
                        subscriber.onNext(event1Arg("ok"));
                        subscriber.onCompleted();
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                        subscriber.onError(e);
                    }
                }));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        vertxServer.stop().toBlocking().subscribe();
        fallbackVertxServer.stop().toBlocking().subscribe();
    }

    private static Event event1Arg(String value) {
        return Event.create("testEvent", MetaData.of("arg", value));
    }

    private static Command command1Arg(String name, String value) {
        return Command.create(name, Pair.of("arg", value));
    }

    @Test
    public void shouldExecuteCommand() throws Exception {
        registry.execute(command1Arg(TEST_COMMAND, "foo"))
                .subscribe(testSubscriber);

        assertCompletedSuccessfully();
        testSubscriber.assertValue(event1Arg("Called command with arg: foo"));
    }

    @Test
    public void shouldCallCommandAndReceiveMultipleEvents() throws Exception {
        registry.execute(command1Arg(TEST_COMMAND_MANY, "bar"))
                .subscribe(testSubscriber);

        assertCompletedSuccessfully();
        testSubscriber.assertValues(
                event1Arg("1. Called command with arg: bar"),
                event1Arg("2. Called command with arg: bar"),
                event1Arg("3. Called command with arg: bar")
        );
    }

    @Test
    public void shouldMainFailAndNoFallbackAvailable() throws Exception {
        registry.execute(command1Arg(TEST_FAIL_COMMAND, "foo"))
                .subscribe(testSubscriber);

        assertError(testSubscriber, RuntimeException.class,
                e -> assertEquals("failed", e.getMessage()));
    }

    @Test
    public void shouldMainFailAndFallbackSucceed() throws Exception {
        registry.execute(command1Arg(TEST_FAIL_BUT_FALLBACK_COMMAND, "foo"))
                .subscribe(testSubscriber);

        assertCompletedSuccessfully();
        testSubscriber.assertValue(event1Arg("Recovered: foo"));
    }

    @Test
    public void shouldComposeDifferentCommands() throws Exception {
        final TestSubscriber<CommandProcessorMetrics> metricsTestSubscriber = new TestSubscriber<>();
        ReactoDashboardStream.observeCommandHandlers()
                .subscribe(metricsTestSubscriber);

        registry.execute(command1Arg(TEST_COMMAND, "foo"))
                .mergeWith(registry.execute(command1Arg(TEST_COMMAND_MANY, "bar")))
                .observeOn(Schedulers.computation())
                .subscribeOn(Schedulers.computation())
                .subscribe(testSubscriber);

        assertCompletedSuccessfully();
        List<Event> onNextEvents = testSubscriber.getOnNextEvents();
        assertEquals("Should be 4 elements", 4, onNextEvents.size());
        assertTrue(onNextEvents.contains(event1Arg("1. Called command with arg: bar")));
        assertTrue(onNextEvents.contains(event1Arg("2. Called command with arg: bar")));
        assertTrue(onNextEvents.contains(event1Arg("3. Called command with arg: bar")));
        assertTrue(onNextEvents.contains(event1Arg("Called command with arg: foo")));

        metricsTestSubscriber.awaitTerminalEventAndUnsubscribeOnTimeout(ReactoDashboardStream.DELAY_IN_MS, TimeUnit.MILLISECONDS);
        metricsTestSubscriber.assertNoErrors();
        metricsTestSubscriber.assertValueCount(1);
        final CommandProcessorMetrics metrics = metricsTestSubscriber.getOnNextEvents().get(0);
        assertEquals(2, metrics.commands().size());
    }

    @Test
    public void shouldFailAfterHystrixTimeout() throws Exception {
        registry.execute(command1Arg(LONG_TASK, "5000"), Event.class, CommandExecutors.hystrix(
                HystrixCommandProperties.defaultSetter()
                        .withExecutionTimeoutEnabled(true)
                        .withExecutionTimeoutInMilliseconds(100)
        )).subscribe(testSubscriber);

        assertActualHystrixError(TimeoutException.class,
                e -> assertEquals("java.util.concurrent.TimeoutException", e.toString()));
    }

    @Test
    public void shouldFailWhenCommandIsInvokedWithInvalidArgument() throws Exception {
        registry.execute(command1Arg(LONG_TASK, "foo"))
                .subscribe(testSubscriber);

        assertError(testSubscriber, NumberFormatException.class,
                e -> assertEquals("For input string: \"foo\"", e.getMessage()));
    }

    @Test
    public void shouldFailAndReceiveCustomExceptionFromCommand() throws Exception {
        registry.execute(command1Arg(COMMAND_CUSTOM_ERROR, "foo"))
                .subscribe(testSubscriber);

        assertError(testSubscriber, CustomError.class,
                customError -> assertEquals("foo", customError.data));
    }

    @Test
    public void shouldCallCommandWithoutArgs() throws Exception {
        registry.execute(Command.create(COMMAND_WITHOUT_ARGS))
                .subscribe(testSubscriber);
        assertCompletedSuccessfully();
        testSubscriber.assertValue(event1Arg("ok"));
    }

    @Test
    public void shouldExecuteHugeCommandEntity() throws Exception {
        String commandWithHugePayload = createDataSize(100_000);

        registry.execute(command1Arg(TEST_COMMAND, commandWithHugePayload))
                .subscribe(testSubscriber);

        assertCompletedSuccessfully();
        testSubscriber.assertValue(event1Arg("Called command with arg: " + commandWithHugePayload));
    }

    @Test
    public void shouldFailWithCommandNotFoundWhenCommandIsNotAvailableOnTheServer() throws Exception {
        registry.execute(Command.create("someUnknownCommand"))
                .subscribe(testSubscriber);

        assertError(testSubscriber, CannotDiscoverService.class,
                cannotDiscoverService -> {});
    }

    @Test
    public void shouldFindServiceAndExecuteCommand() throws Exception {
        registry.execute(command1Arg(TEST_COMMAND, "foo"))
                .subscribe(testSubscriber);

        assertCompletedSuccessfully();

        testSubscriber.assertValue(event1Arg("Called command with arg: foo"));

        registry.execute(command1Arg(TEST_COMMAND, "foo"))
                .subscribe(testSubscriber);

        assertCompletedSuccessfully();
        testSubscriber.assertValue(event1Arg("Called command with arg: foo"));
    }

    @Test
    public void shouldNotFindService() throws Exception {
        registry.execute(Command.create("unknown"))
            .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertError(CannotDiscoverService.class);
    }

    @Test
    public void shouldFindAndExecuteCommand() throws Exception {
        registry.execute(command1Arg(TEST_COMMAND, "foo"))
                .subscribe(testSubscriber);

        assertCompletedSuccessfully();
        testSubscriber.assertValue(event1Arg("Called command with arg: foo"));

        final TestSubscriber<Event> testSubscriber2 = new TestSubscriber<>();

        registry.execute(command1Arg(TEST_COMMAND, "bar"))
                .subscribe(testSubscriber2);

        assertCompletedSuccessfully(testSubscriber2);
        testSubscriber2.assertValue(event1Arg("Called command with arg: bar"));
    }

    @Test
    public void shouldExecuteTypedCommandAndReceiveTypedEvent() throws Exception {
        registry.execute(new MakeDemo("Hello, World!"), DemoMade.class)
                .subscribe(typedSubscriber);

        assertCompletedSuccessfully(typedSubscriber);
        typedSubscriber.assertValue(new DemoMade("Hello, World!"));
    }

    @Test
    public void shouldFailWhenExecutingTypedCommand() throws Exception {
        final TestSubscriber<JacksonEvent> jacksonEventTestSubscriber = new TestSubscriber<>();
        registryTyped.execute(new JacksonCommand("test"), JacksonEvent.class)
                .subscribe(jacksonEventTestSubscriber);

        assertError(jacksonEventTestSubscriber, RuntimeException.class,
                e -> assertEquals("test error", e.getMessage()));
    }

    @Test
    public void shouldExecuteTypedCommandWithIncompatibleEventClass() throws Exception {
        final TestSubscriber<Foo> fooTestSubscriber = new TestSubscriber<>();
        registry.execute(new MakeDemo("Hello, World!"), Foo.class)
                .subscribe(fooTestSubscriber);

        fooTestSubscriber.awaitTerminalEvent();
        fooTestSubscriber.assertNoValues();
        fooTestSubscriber.assertError(CannotDiscoverService.class);
        fooTestSubscriber.assertNotCompleted();
    }

    @Test
    public void shouldExecuteTypedCommandAndReceivePolymorphicEvents() throws Exception {
        final TestSubscriber<Animal> animalTestSubscriber = new TestSubscriber<>();
        registryTyped.execute(new Feed("Pedigree"), Animal.class)
                .subscribe(animalTestSubscriber);

        assertCompletedSuccessfully(animalTestSubscriber);
        animalTestSubscriber.assertValues(
                new Dog("Dog ate Pedigree"),
                new Cat("Cat ate Pedigree")
        );
    }

    @Test
    public void shouldExecutePlainAsTyped() throws Exception {
        registry.execute(command1Arg(TEST_COMMAND, "foo"), Event.class)
                .subscribe(testSubscriber);
        assertCompletedSuccessfully();
        testSubscriber.assertValue(event1Arg("Called command with arg: foo"));
    }

    @Test
    public void shouldFailWhenConnectingToInExistingWebSocketStream() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final AtomicReference<Throwable> ex = new AtomicReference<>();
        final HttpClient httpClient = Vertx.vertx().createHttpClient(new HttpClientOptions().setSsl(false));
        httpClient.websocket(MAIN_SERVER_PORT, "localhost", "/undefined/",
                websocket -> websocket
                        .exceptionHandler(e -> fail(e.toString()))
                        .handler(buffer -> fail("handler"))
                        .frameHandler(buffer -> fail("frame handler"))
                        .endHandler(event -> fail("ended"))
                        .closeHandler(event -> fail("closed"))
                , failure -> {
                    ex.set(failure);
                    countDownLatch.countDown();
                });

        countDownLatch.await(1000L, TimeUnit.MILLISECONDS);
        assertEquals(WebSocketHandshakeException.class, ex.get().getClass());
    }

    private Observable<String> get(int port, String host, String uri) {
        final HttpClient httpClient = Vertx.vertx().createHttpClient(new HttpClientOptions().setSsl(false));
        return Observable.create(subscriber ->
                httpClient.getNow(port, host, uri,
                        response -> response
                                .exceptionHandler(subscriber::onError)
                                .bodyHandler(buffer -> {
                                    subscriber.onNext(buffer.toString());
                                    subscriber.onCompleted();
                                })));
    }

    private void assertCompletedSuccessfully() {
        assertCompletedSuccessfully(testSubscriber);
    }

    private  <T> void assertCompletedSuccessfully(TestSubscriber<T> testSubscriber) {
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
    }


    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "unchecked"})
    private <T extends Throwable> void assertError(TestSubscriber<?> testSubscriber, Class<T> expected, Consumer<T> errorChecker) {
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNotCompleted();
        final List<Throwable> onErrorEvents = testSubscriber.getOnErrorEvents();
        assertEquals("Should be one error", 1, onErrorEvents.size());

        final Throwable throwable = onErrorEvents.get(0);
        assertEquals("Should be HystrixRuntimeException", expected, throwable.getClass());
        final Throwable actualCause = throwable.getCause();
        errorChecker.accept((T) throwable);
    }

    private <T extends Throwable> void assertActualHystrixError(Class<T> expected, Consumer<T> errorChecker) {
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNotCompleted();
        final List<Throwable> onErrorEvents = testSubscriber.getOnErrorEvents();
        assertEquals("Should be one error", 1, onErrorEvents.size());

        final Throwable throwable = onErrorEvents.get(0);
        assertEquals("Should be HystrixRuntimeException", HystrixRuntimeException.class, throwable.getClass());
        final Throwable actualCause = throwable.getCause();
        assertTrue("Actual: " + actualCause.getClass() + ".Expected: " + expected,
                expected.isAssignableFrom(actualCause.getClass()));
        errorChecker.accept(expected.cast(actualCause));
    }

    private static String createDataSize(int msgSize) {
        StringBuilder sb = new StringBuilder(msgSize);
        for (int i=0; i<msgSize; i++) {
            sb.append('a');
        }
        return sb.toString();
    }
}
