package io.reacto.client.commands;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import io.reacto.TestUtils.models.CustomError;
import io.reacto.client.errors.CommandNotFound;
import io.reacto.server.CommandRegistry;
import io.reacto.server.VertxServer;
import io.reacto.types.Command;
import io.reacto.types.Event;
import io.reacto.types.MetaData;
import io.reacto.types.Pair;
import io.reacto.utils.Factories;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import io.reacto.client.errors.ConnectionClosedUnexpectedly;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    private static final String FALLBACK_NODE = "http://localhost:8383/distFallback/";

    private static HttpServer mainHttpServer;
    private static VertxServer vertxServer;
    private static VertxServer fallbackVertxServer;

    private final TestSubscriber<Event> testSubscriber = new TestSubscriber<>();
    private final CommandExecutor mainNodeExecutor = CommandExecutors.webSocket(Nodes.ofMain(MAIN_NODE));
    private final CommandExecutor mainNodeAndFallbackExecutor = CommandExecutors.webSocket(Nodes.ofMainAndFallback(MAIN_NODE, FALLBACK_NODE));

    @BeforeClass
    public static void setUp() throws Exception {
        CommandRegistry mainCommands = CommandRegistry.of(TEST_COMMAND, cmd ->
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

        Vertx vertx = Factories.vertx();
        mainHttpServer = vertx.createHttpServer(new HttpServerOptions()
                .setPort(8282)
                .setSsl(false)
                .setReuseAddress(true));

        HttpServer fallbackHttpServer = vertx.createHttpServer(new HttpServerOptions()
                .setPort(8383)
                .setSsl(false)
                .setReuseAddress(true));
        vertxServer = new VertxServer(Router.router(vertx), mainHttpServer, "dist/", mainCommands);
        fallbackVertxServer = new VertxServer(Router.router(vertx), fallbackHttpServer, "distFallback/", fallbackCommands);
        fallbackVertxServer.start();
        vertxServer.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        vertxServer.stop();
        fallbackVertxServer.stop();
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
        CommandExecutor sut = CommandExecutors.webSocket(Nodes.ofMain(MAIN_NODE), CommandExecutors.DEFAULT_EXECUTION_TIMEOUT);
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
        CommandExecutor sut = CommandExecutors.webSocket(Nodes.ofMain("http://localhost:45689/foo/"));
        sut.execute(command1Arg(TEST_COMMAND, "foo"))
            .subscribe(testSubscriber);

        assertActualHystrixError(ConnectException.class,
                e -> assertEquals("Connection refused: no further information: localhost/127.0.0.1:45689", e.getMessage()));
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
    public void shouldReceiveOneEventAndThenFail() throws Exception {
        mainNodeExecutor.execute(Command.create(COMMAND_EMIT_AND_FAIL))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent(500L, TimeUnit.MILLISECONDS);
        testSubscriber.assertValue(Event.create("ok"));
        //shut down main node
        mainHttpServer.close();
        try {
            assertActualHystrixError(ConnectionClosedUnexpectedly.class, connectionClosedUnexpectedly ->
                    assertTrue(connectionClosedUnexpectedly.getMessage()
                            .startsWith("WebSocket connection closed without completion for command: ")));
        } finally {
            mainHttpServer.listen();
        }
    }

    private void assertCompletedSuccessfully() {
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
        assertEquals(expected, actualCause.getClass());
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
