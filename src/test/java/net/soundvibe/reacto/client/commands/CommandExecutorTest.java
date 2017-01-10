package net.soundvibe.reacto.client.commands;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.servicediscovery.*;
import io.vertx.servicediscovery.types.HttpEndpoint;
import net.soundvibe.reacto.client.errors.*;
import net.soundvibe.reacto.discovery.*;
import net.soundvibe.reacto.mappers.Mappers;
import net.soundvibe.reacto.server.*;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.utils.*;
import net.soundvibe.reacto.utils.models.CustomError;
import org.junit.*;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/**
 * @author Cipolinas on 2015.12.01.
 */
public class CommandExecutorTest {

    private static final String TEST_COMMAND = "test";
    private static final String TEST_COMMAND_MANY = "testMany";
    private static final String TEST_FAIL_COMMAND = "testFail";
    private static final String TEST_FAIL_BUT_FALLBACK_COMMAND = "testFailFallback";
    private static final String LONG_TASK = "longTask";
    private static final String COMMAND_WITHOUT_ARGS = "argLessCommand";
    private static final String COMMAND_CUSTOM_ERROR = "commandCustomError";
    private static final String COMMAND_EMIT_AND_FAIL = "emitAndFail";

    private static final String MAIN_NODE = "http://localhost:8282/dist/";
    private static final String FALLBACK_NODE = "http://localhost:8383/dist/";
    private static final int MAIN_SERVER_PORT = 8282;
    private static final int FALLBACK_SERVER_PORT = 8383;

    private static HttpServer mainHttpServer;
    private static VertxServer vertxServer;
    private static VertxServer fallbackVertxServer;
    private static ServiceDiscovery serviceDiscovery;
    private static ReactoServiceRegistry reactoServiceRegistry;

    private final TestSubscriber<Event> testSubscriber = new TestSubscriber<>();
    private final CommandExecutor mainNodeExecutor = CommandExecutors.webSocket(
            Nodes.of(MAIN_NODE), CommandExecutors.defaultHystrixSetter());
    private final CommandExecutor mainNodeAndFallbackExecutor = CommandExecutors.webSocket(Nodes.of(MAIN_NODE, FALLBACK_NODE));
    private static CommandRegistry mainCommands;
    private static Vertx vertx;

    @BeforeClass
    public static void setUp() throws Exception {
        mainCommands = CommandRegistry.ofTyped(MakeDemo.class, DemoMade.class,
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
                }))
        ;

        CommandRegistry fallbackCommands = CommandRegistry.of(TEST_FAIL_BUT_FALLBACK_COMMAND,
                o -> event1Arg("Recovered: " + o.get("arg")).toObservable());

        vertx = Vertx.vertx();
        serviceDiscovery = ServiceDiscovery.create(vertx);
        reactoServiceRegistry = new ReactoServiceRegistry(serviceDiscovery, new DemoServiceRegistryMapper());
        ReactoServiceRegistry reactoServiceRegistry2 = new ReactoServiceRegistry(serviceDiscovery, new DemoServiceRegistryMapper());

        mainHttpServer = vertx.createHttpServer(new HttpServerOptions()
                .setPort(MAIN_SERVER_PORT)
                .setSsl(false)
                .setReuseAddress(true));

        HttpServer fallbackHttpServer = vertx.createHttpServer(new HttpServerOptions()
                .setPort(FALLBACK_SERVER_PORT)
                .setSsl(false)
                .setReuseAddress(true));

        final Router router = Router.router(vertx);
        router.route("/health").handler(event -> event.response().end("ok"));
        System.out.println("Before starting servers...");
        vertxServer = new VertxServer(new ServiceOptions("dist", "dist/", "0.1")
                , router, mainHttpServer, mainCommands, reactoServiceRegistry);
        fallbackVertxServer = new VertxServer(new ServiceOptions("dist","dist/", "0.1")
                , Router.router(vertx), fallbackHttpServer,  fallbackCommands, reactoServiceRegistry2);
        fallbackVertxServer.start().toBlocking().subscribe();
        vertxServer.start().toBlocking().subscribe();
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
        mainNodeExecutor.execute(command1Arg(TEST_COMMAND, "foo"))
                .subscribe(testSubscriber);

        assertCompletedSuccessfully();
        testSubscriber.assertValue(event1Arg("Called command with arg: foo"));
    }

    @Test
    public void shouldCallCommandAndReceiveMultipleEvents() throws Exception {
        mainNodeExecutor.execute(command1Arg(TEST_COMMAND_MANY, "bar"))
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
        mainNodeExecutor.execute(command1Arg(TEST_FAIL_COMMAND, "foo"))
                .subscribe(testSubscriber);

        assertActualHystrixError(RuntimeException.class,
                e -> assertEquals("failed", e.getMessage()));
    }

    @Test
    public void shouldMainFailAndFallbackSucceed() throws Exception {
        mainNodeAndFallbackExecutor.execute(command1Arg(TEST_FAIL_BUT_FALLBACK_COMMAND, "foo"))
                .subscribe(testSubscriber);

        assertCompletedSuccessfully();
        testSubscriber.assertValue(event1Arg("Recovered: foo"));
    }

    @Test
    public void shouldComposeDifferentCommands() throws Exception {
        mainNodeExecutor.execute(command1Arg(TEST_COMMAND, "foo"))
                .mergeWith(mainNodeExecutor.execute(command1Arg(TEST_COMMAND_MANY, "bar")))
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
    }

    @Test
    public void shouldFailAfterHystrixTimeout() throws Exception {
        CommandExecutor sut = CommandExecutors.webSocket(Nodes.of(MAIN_NODE), 1000);
        sut.execute(command1Arg(LONG_TASK, "5000"))
                .subscribe(testSubscriber);

        assertActualHystrixError(TimeoutException.class,
                e -> assertEquals("java.util.concurrent.TimeoutException", e.toString()));
    }

    @Test
    public void shouldFailWhenCommandIsInvokedWithInvalidArgument() throws Exception {
        mainNodeExecutor.execute(command1Arg(LONG_TASK, "foo"))
                .subscribe(testSubscriber);

        assertActualHystrixError(NumberFormatException.class,
                e -> assertEquals("For input string: \"foo\"", e.getMessage()));
    }

    @Test
    public void shouldFailAndReceiveCustomExceptionFromCommand() throws Exception {
        mainNodeExecutor.execute(command1Arg(COMMAND_CUSTOM_ERROR, "foo"))
                .subscribe(testSubscriber);

        assertActualHystrixError(CustomError.class,
                customError -> assertEquals("foo", customError.data));
    }

    @Test
    public void shouldCallCommandWithoutArgs() throws Exception {
        mainNodeExecutor.execute(Command.create(COMMAND_WITHOUT_ARGS))
                .subscribe(testSubscriber);
        assertCompletedSuccessfully();
        testSubscriber.assertValue(event1Arg("ok"));
    }

    @Test
    public void shouldFailWhenCommandExecutorIsInaccessible() throws Exception {
        CommandExecutor sut = CommandExecutors.webSocket(Nodes.of("http://localhost:45689/foo/"), 5000);
        sut.execute(command1Arg(TEST_COMMAND, "foo"))
            .subscribe(testSubscriber);

        assertActualHystrixError(ConnectException.class,
                e -> assertFalse(e.getMessage().isEmpty()));
    }

    @Test
    public void shouldExecuteHugeCommandEntity() throws Exception {
        String commandWithHugePayload = createDataSize(100_000);

        mainNodeExecutor.execute(command1Arg(TEST_COMMAND, commandWithHugePayload))
                .subscribe(testSubscriber);

        assertCompletedSuccessfully();
        testSubscriber.assertValue(event1Arg("Called command with arg: " + commandWithHugePayload));
    }

    @Test
    public void shouldFailWithCommandNotFoundWhenCommandIsNotAvailableOnTheServer() throws Exception {
        mainNodeExecutor.execute(Command.create("someUnknownCommand"))
                .subscribe(testSubscriber);

        assertActualHystrixError(CommandNotFound.class,
                commandNotFound -> assertEquals("Command not found: someUnknownCommand", commandNotFound.getMessage()));
    }

    @Test
    public void shouldEmitOneEventAndThenFail() throws Exception {
        final Vertx vertx = Vertx.vertx();
        final ServiceDiscovery serviceDiscovery = ServiceDiscovery.create(vertx);
        final HttpServer server = vertx.createHttpServer(new HttpServerOptions()
                .setPort(8183)
                .setSsl(false)
                .setReuseAddress(true));

        final VertxServer reactoServer = new VertxServer(new ServiceOptions("distTest", "distTest/")
                , Router.router(vertx), server,
                CommandRegistry.of(COMMAND_EMIT_AND_FAIL,
                command -> Observable.create(subscriber -> {
                    subscriber.onNext(Event.create("ok"));
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                })),
                new ReactoServiceRegistry(serviceDiscovery, Mappers.untypedServiceRegistryMapper()));

        final CommandExecutor executor = CommandExecutors.webSocket(Nodes.of("http://localhost:8183/distTest/"), 5000);

        reactoServer.start().toBlocking().subscribe();

        try {
            executor.execute(Command.create(COMMAND_EMIT_AND_FAIL))
                    .subscribe(testSubscriber);

            testSubscriber.awaitTerminalEvent(500L, TimeUnit.MILLISECONDS);


        } finally {
            reactoServer.stop().toBlocking().subscribe();
            Thread.sleep(100L);
        }

        //assertEquals(Event.create("ok"), testSubscriber.getOnNextEvents().get(0));
        assertActualHystrixError(ConnectionClosedUnexpectedly.class, connectionClosedUnexpectedly ->
                assertTrue(connectionClosedUnexpectedly.getMessage()
                        .startsWith("WebSocket connection closed without completion for command: ")));
    }

    @Test
    public void shouldFindServiceAndExecuteCommand() throws Exception {
        CommandExecutors.find(Service.of("dist", serviceDiscovery))
                .flatMap(commandExecutor -> commandExecutor.execute(command1Arg(TEST_COMMAND, "foo")))
                .subscribe(testSubscriber);

        assertCompletedSuccessfully();

        testSubscriber.assertValue(event1Arg("Called command with arg: foo"));


        CommandExecutors.find(Service.of("dist", serviceDiscovery))
                .flatMap(commandExecutor -> commandExecutor.execute(command1Arg(TEST_COMMAND, "foo")))
                .subscribe(testSubscriber);
        assertCompletedSuccessfully();
        testSubscriber.assertValue(event1Arg("Called command with arg: foo"));
    }


    @Test
    public void shouldNotFindService() throws Exception {
        final TestSubscriber<CommandExecutor> subscriber = new TestSubscriber<>();

        CommandExecutors.find(Service.of("NotExists", serviceDiscovery))
            .subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        subscriber.assertError(CannotDiscoverService.class);
    }

    @Test
    public void shouldFindServicesAndBalanceTheLoad() throws Exception {
        //start new service
        final HttpServer server = vertx.createHttpServer(new HttpServerOptions()
                .setPort(8183)
                .setSsl(false)
                .setReuseAddress(true));

        final VertxServer reactoServer = new VertxServer(new ServiceOptions("dist", "dist/")
                , Router.router(vertx), server,
                CommandRegistry.of(TEST_COMMAND, cmd ->
                        event1Arg("Called command from second server with arg: "
                                + cmd.get("arg")).toObservable()),
                new ReactoServiceRegistry(serviceDiscovery, Mappers.untypedServiceRegistryMapper()));
        reactoServer.start().toBlocking().subscribe();

        try {
            final Service service = Service.of("dist", serviceDiscovery);

            CommandExecutors.find(service)
                    .flatMap(commandExecutor -> commandExecutor.execute(command1Arg(TEST_COMMAND, "foo")))
                    .subscribe(testSubscriber);

            assertCompletedSuccessfully();

            testSubscriber.assertValueCount(1);
            final Event actual = testSubscriber.getOnNextEvents().get(0);
            assertTrue(actual.equals(event1Arg("Called command from second server with arg: foo")) ||
                actual.equals(event1Arg("Called command with arg: foo")));


            TestSubscriber<Event> eventTestSubscriber = new TestSubscriber<>();

            CommandExecutors.find(service)
                    .flatMap(commandExecutor -> commandExecutor.execute(command1Arg(TEST_COMMAND, "bar")))
                    .subscribe(eventTestSubscriber);
            eventTestSubscriber.awaitTerminalEvent();
            eventTestSubscriber.assertNoErrors();
            eventTestSubscriber.assertCompleted();
            eventTestSubscriber.assertValueCount(1);

            final Event actual2 = eventTestSubscriber.getOnNextEvents().get(0);
            assertTrue(actual2.equals(event1Arg("Called command with arg: bar")) ||
                actual2.equals(event1Arg("Called command from second server with arg: bar")));
        } finally {
            reactoServer.stop().toBlocking().subscribe();
            Thread.sleep(100L);
        }
    }

    @Test
    public void shouldFindAndExecuteCommand() throws Exception {
        reactoServiceRegistry.execute(command1Arg(TEST_COMMAND, "foo"))
                .subscribe(testSubscriber);

        assertCompletedSuccessfully();
        testSubscriber.assertValue(event1Arg("Called command with arg: foo"));

        final TestSubscriber<Event> testSubscriber2 = new TestSubscriber<>();

        reactoServiceRegistry.execute(command1Arg(TEST_COMMAND, "bar"))
                .subscribe(testSubscriber2);

        assertCompletedSuccessfully(testSubscriber2);
        testSubscriber2.assertValue(event1Arg("Called command with arg: bar"));
    }

    @Test
    public void shouldExecuteTypedCommandAndReceiveTypedEvent() throws Exception {
        final TestSubscriber<DemoMade> typedSubscriber = new TestSubscriber<>();
        reactoServiceRegistry.execute(new MakeDemo("Hello, World!"), DemoMade.class)
                .subscribe(typedSubscriber);

        assertCompletedSuccessfully(typedSubscriber);
        typedSubscriber.assertValue(new DemoMade("Hello, World!"));
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

    @Test
    public void shouldCloseOpenServiceDiscovery() throws Exception {
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();

        get(MAIN_SERVER_PORT, "localhost", "/dist/service-discovery/close")
                .subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();

        get(FALLBACK_SERVER_PORT, "localhost", "/dist/service-discovery/close")
                .toBlocking().subscribe();

        System.out.println("Response: " + testSubscriber.getOnNextEvents());
        final Record actual = new Record(new JsonObject(testSubscriber.getOnNextEvents().get(0)));
        assertEquals("dist", actual.getName());
        assertEquals(HttpEndpoint.TYPE, actual.getType());

        TestSubscriber<CommandExecutor> testSubscriber2 = new TestSubscriber<>();

        DiscoverableServices.find("dist", serviceDiscovery)
                .subscribe(testSubscriber2);

        testSubscriber2.awaitTerminalEvent();
        testSubscriber2.assertError(CannotDiscoverService.class);

        get(MAIN_SERVER_PORT, "localhost", "/dist/service-discovery/start")
                .toBlocking().subscribe();

        get(FALLBACK_SERVER_PORT, "localhost", "/dist/service-discovery/start")
                .toBlocking().subscribe();


        TestSubscriber<CommandExecutor> testSubscriber3 = new TestSubscriber<>();
        DiscoverableServices.find("dist", serviceDiscovery)
                .subscribe(testSubscriber3);

        testSubscriber3.awaitTerminalEvent();
        testSubscriber3.assertNoErrors();
        testSubscriber3.assertValueCount(1);
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
        errorChecker.accept((T) actualCause);
    }

    private static String createDataSize(int msgSize) {
        StringBuilder sb = new StringBuilder(msgSize);
        for (int i=0; i<msgSize; i++) {
            sb.append('a');
        }
        return sb.toString();
    }
}
