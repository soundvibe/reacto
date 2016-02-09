package reactive.fp.client.commands;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import reactive.TestUtils.models.CustomError;
import reactive.fp.server.CommandRegistry;
import reactive.fp.server.VertxServer;
import reactive.fp.types.*;
import reactive.fp.utils.Factories;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.net.ConnectException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static reactive.fp.client.commands.CommandExecutors.DEFAULT_EXECUTION_TIMEOUT;
import static reactive.fp.client.commands.Nodes.ofMain;
import static reactive.fp.client.commands.Nodes.ofMainAndFallback;

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

    private static final String MAIN_NODE = "http://localhost:8282/dist/";
    private static final String FALLBACK_NODE = "http://localhost:8383/distFallback/";


    private static VertxServer vertxServer;
    private static VertxServer fallbackVertxServer;

    private final TestSubscriber<Event> testSubscriber = new TestSubscriber<>();
    private final CommandExecutor mainNodeExecutor = CommandExecutors.webSocket(ofMain(MAIN_NODE));
    private final CommandExecutor mainNodeAndFallbackExecutor = CommandExecutors.webSocket(ofMainAndFallback(MAIN_NODE, FALLBACK_NODE));

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
        HttpServer mainHttpServer = vertx.createHttpServer(new HttpServerOptions()
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

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertValue(event1Arg("Called command with arg: foo"));
    }


    @Test
    public void shouldCallCommandAndReceiveMultipleEvents() throws Exception {
        mainNodeExecutor.execute(command1Arg(TEST_COMMAND_MANY, "bar"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
        testSubscriber.assertValues(
                event1Arg("1. Called command with arg: bar"),
                event1Arg("2. Called command with arg: bar"),
                event1Arg("3. Called command with arg: bar")
        );
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Test
    public void shouldMainFailAndNoFallbackAvailable() throws Exception {
        mainNodeExecutor.execute(command1Arg(TEST_FAIL_COMMAND, "foo"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoValues();
        List<Throwable> errors = testSubscriber.getOnErrorEvents();
        assertEquals("Should be one error", 1, errors.size());
        Throwable throwable = errors.get(0);
        assertEquals("Error message should be failed", "testFail$ failed and fallback disabled.", throwable.getMessage());
        assertEquals("Should be HystrixRuntimeException", HystrixRuntimeException.class, throwable.getClass());
    }

    @Test
    public void shouldMainFailAndFallbackSucceed() throws Exception {
        mainNodeAndFallbackExecutor.execute(command1Arg(TEST_FAIL_BUT_FALLBACK_COMMAND, "foo"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
        testSubscriber.assertValue(event1Arg("Recovered: foo"));
    }

    @Test
    public void shouldComposeDifferentCommands() throws Exception {
        mainNodeExecutor.execute(command1Arg(TEST_COMMAND, "foo"))
                .mergeWith(mainNodeExecutor.execute(command1Arg(TEST_COMMAND_MANY, "bar")))
                .observeOn(Schedulers.computation())
                .subscribeOn(Schedulers.computation())
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
        List<Event> onNextEvents = testSubscriber.getOnNextEvents();
        assertEquals("Should be 4 elements", 4, onNextEvents.size());
        assertTrue(onNextEvents.contains(event1Arg("1. Called command with arg: bar")));
        assertTrue(onNextEvents.contains(event1Arg("2. Called command with arg: bar")));
        assertTrue(onNextEvents.contains(event1Arg("3. Called command with arg: bar")));
        assertTrue(onNextEvents.contains(event1Arg("Called command with arg: foo")));
    }

    @Test
    public void shouldFailAfterHystrixTimeout() throws Exception {
        CommandExecutor sut = CommandExecutors.webSocket(ofMain(MAIN_NODE), DEFAULT_EXECUTION_TIMEOUT);
        sut.execute(command1Arg(LONG_TASK, "5000"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNotCompleted();
        testSubscriber.assertNoValues();
        testSubscriber.assertError(HystrixRuntimeException.class);
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Test
    public void shouldFailWhenCommandIsInvokedWithInvalidArgument() throws Exception {
        mainNodeExecutor.execute(command1Arg(LONG_TASK, "foo"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNotCompleted();
        testSubscriber.assertNoValues();
        testSubscriber.assertError(HystrixRuntimeException.class);
        final Throwable actualError = testSubscriber.getOnErrorEvents().get(0).getCause();
        assertEquals(NumberFormatException.class, actualError.getClass());
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Test
    public void shouldFailAndReceiveCustomExceptionFromCommand() throws Exception {
        mainNodeExecutor.execute(command1Arg(COMMAND_CUSTOM_ERROR, "foo"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNotCompleted();
        testSubscriber.assertNoValues();
        testSubscriber.assertError(HystrixRuntimeException.class);
        final Throwable actualError = testSubscriber.getOnErrorEvents().get(0).getCause();
        assertEquals(CustomError.class, actualError.getClass());
        assertEquals("foo", ((CustomError) actualError).data);
    }


    @Test
    public void shouldCallCommandWithoutArgs() throws Exception {
        mainNodeExecutor.execute(Command.create(COMMAND_WITHOUT_ARGS))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(event1Arg("ok"));
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Test
    public void shouldFailWhenCommandExecutorIsInaccessible() throws Exception {
        CommandExecutor sut = CommandExecutors.webSocket(ofMain("http://localhost:45689/foo/"));
        sut.execute(command1Arg(TEST_COMMAND, "foo"))
            .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        List<Throwable> onErrorEvents = testSubscriber.getOnErrorEvents();
        assertEquals(ConnectException.class, onErrorEvents.get(0).getCause().getClass());
        testSubscriber.assertNotCompleted();
        testSubscriber.assertNoValues();
        testSubscriber.assertError(HystrixRuntimeException.class);
    }

    @Test
    public void shouldExecuteHugeCommandEntity() throws Exception {
        String commandWithHugePayload = createDataSize(100_000);

        mainNodeExecutor.execute(command1Arg(TEST_COMMAND, commandWithHugePayload))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(event1Arg("Called command with arg: " + commandWithHugePayload));
    }

    private static String createDataSize(int msgSize) {
        StringBuilder sb = new StringBuilder(msgSize);
        for (int i=0; i<msgSize; i++) {
            sb.append('a');
        }
        return sb.toString();
    }
}
