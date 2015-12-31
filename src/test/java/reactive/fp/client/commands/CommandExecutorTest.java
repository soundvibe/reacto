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
import reactive.TestUtils.models.Foo;
import reactive.TestUtils.models.FooBar;
import reactive.TestUtils.models.NotDeserializable;
import reactive.fp.server.CommandRegistry;
import reactive.fp.server.VertxServer;
import reactive.fp.utils.Factories;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static reactive.fp.client.commands.CommandDef.ofMain;
import static reactive.fp.client.commands.CommandDef.ofMainAndFallback;

/**
 * @author Cipolinas on 2015.12.01.
 */
public class CommandExecutorTest {

    public static final String TEST_COMMAND = "test";
    public static final String TEST_COMMAND_MANY = "testMany";
    public static final String TEST_FAIL_COMMAND = "testFail";
    public static final String TEST_FAIL_BUT_FALLBACK_COMMAND = "testFailFallback";
    public static final String LONG_TASK = "longTask";
    public static final String COMMAND_WITHOUT_ARGS = "argLessCommand";
    public static final String COMMAND_OO = "commandOO";
    public static final String COMMAND_NOT_DESERIALIZABLE = "commandNotDeserializable";
    public static final String COMMAND_CUSTOM_ERROR = "commandCustomError";

    public static final String MAIN_NODE = "http://localhost:8282/dist/";
    public static final String FALLBACK_NODE = "http://localhost:8383/distFallback/";


    private static VertxServer vertxServer;
    private static VertxServer fallbackVertxServer;

    @BeforeClass
    public static void setUp() throws Exception {
        CommandRegistry mainCommands = CommandRegistry.of(TEST_COMMAND, o -> Observable.just("Called command with arg: " + o))
                .and(TEST_COMMAND_MANY, (String o) -> Observable.just(
                        "1. Called command with arg: " + o,
                        "2. Called command with arg: " + o,
                        "3. Called command with arg: " + o
                ))
                .and(TEST_FAIL_COMMAND, o -> Observable.error(new RuntimeException("failed")))
                .and(TEST_FAIL_BUT_FALLBACK_COMMAND, o -> Observable.error(new RuntimeException("failed")))
                .and(COMMAND_WITHOUT_ARGS, o -> Observable.just("ok"))
                .and(COMMAND_OO, (String o) -> Observable.just(new FooBar(o, o)))
                .and(COMMAND_NOT_DESERIALIZABLE, (String o) -> Observable.just(new NotDeserializable(o)))
                .and(COMMAND_CUSTOM_ERROR, (String o) -> Observable.error(new CustomError(o)))
                .and(LONG_TASK, (Integer interval) -> Observable.create(subscriber -> {
                    try {
                        Thread.sleep(interval);
                        subscriber.onNext("ok");
                        subscriber.onCompleted();
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                        //subscriber.onError(e);
                    }
                }))
                ;

        CommandRegistry fallbackCommands = CommandRegistry.of(TEST_FAIL_BUT_FALLBACK_COMMAND, o -> Observable.just("Recovered: " + o));

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

    @Test
    public void shouldExecuteCommand() throws Exception {
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        CommandExecutor<String> sut = CommandExecutors.webSocket(ofMain(TEST_COMMAND, MAIN_NODE, String.class));
        sut.execute("foo")
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertValue("Called command with arg: foo");
    }


    @Test
    public void shouldCallCommandAndReceiveMultipleEvents() throws Exception {
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        CommandExecutor<String> sut = CommandExecutors.webSocket(ofMain(TEST_COMMAND_MANY, MAIN_NODE, String.class));
        sut.execute("bar")
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
        testSubscriber.assertValues(
                "1. Called command with arg: bar",
                "2. Called command with arg: bar",
                "3. Called command with arg: bar"
        );
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Test
    public void shouldMainFailAndNoFallbackAvailable() throws Exception {
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        CommandExecutor<String> sut = CommandExecutors.webSocket(ofMain(TEST_FAIL_COMMAND, MAIN_NODE, String.class));
        sut.execute("foo")
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
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        CommandExecutor<String> sut = CommandExecutors.webSocket(ofMainAndFallback(TEST_FAIL_BUT_FALLBACK_COMMAND, MAIN_NODE, FALLBACK_NODE, String.class));
        sut.execute("foo")
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
        testSubscriber.assertValue("Recovered: foo");
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Test
    public void shouldFailWhenPayloadIsOfInvalidClass() throws Exception {
        TestSubscriber<Integer> testSubscriber = new TestSubscriber<>();
        CommandExecutor<Integer> sut = CommandExecutors.webSocket(ofMain(TEST_COMMAND, MAIN_NODE, Integer.class));
        sut.execute("foo")
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertError(HystrixRuntimeException.class);
        testSubscriber.assertNotCompleted();
        testSubscriber.assertNoValues();
        assertEquals(ClassCastException.class, testSubscriber.getOnErrorEvents().get(0).getCause().getClass());
    }

    @Test
    public void shouldFailWhenCannotDeserializeReceivedEvent() throws Exception {
        TestSubscriber<NotDeserializable> testSubscriber = new TestSubscriber<>();
        CommandExecutor<NotDeserializable> sut = CommandExecutors.webSocket(ofMain(COMMAND_NOT_DESERIALIZABLE, MAIN_NODE, NotDeserializable.class));
        sut.execute("bar")
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        System.out.println(testSubscriber.getOnNextEvents());
        testSubscriber.assertNotCompleted();
        testSubscriber.assertError(HystrixRuntimeException.class);
    }

    @Test
    public void shouldReceiveEventsAsSubTypeOfTheTypeCommandIsEmitting() throws Exception {
        TestSubscriber<Foo> testSubscriber = new TestSubscriber<>();
        CommandExecutor<Foo> sut = CommandExecutors.webSocket(ofMain(COMMAND_OO, MAIN_NODE, Foo.class));
        sut.execute("bar")
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertValue(new Foo("bar"));
    }

    @Test
    public void shouldComposeDifferentCommands() throws Exception {
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        CommandExecutor<String> sut1 = CommandExecutors.webSocket(ofMain(TEST_COMMAND, MAIN_NODE, String.class));
        CommandExecutor<String> sut2 = CommandExecutors.webSocket(ofMain(TEST_COMMAND_MANY, MAIN_NODE, String.class));
        sut1.execute("foo")
                .mergeWith(sut2.execute("bar"))
                .observeOn(Schedulers.computation())
                .subscribeOn(Schedulers.computation())
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
        List<String> onNextEvents = testSubscriber.getOnNextEvents();
        assertEquals("Should be 4 elements", 4, onNextEvents.size());
        assertTrue(onNextEvents.contains("1. Called command with arg: bar"));
        assertTrue(onNextEvents.contains("2. Called command with arg: bar"));
        assertTrue(onNextEvents.contains("3. Called command with arg: bar"));
        assertTrue(onNextEvents.contains("Called command with arg: foo"));
    }

    @Test
    public void shouldFailAfterHystrixTimeout() throws Exception {
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        CommandExecutor<String> sut = CommandExecutors.webSocket(ofMain(LONG_TASK, MAIN_NODE, String.class));
        sut.observe(5000)
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNotCompleted();
        testSubscriber.assertNoValues();
        testSubscriber.assertError(HystrixRuntimeException.class);
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Test
    public void shouldFailWhenCommandIsInvokedWithInvalidArgument() throws Exception {
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        CommandExecutor<String> sut = CommandExecutors.webSocket(ofMain(LONG_TASK, MAIN_NODE, String.class));
        sut.execute("foo")
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNotCompleted();
        testSubscriber.assertNoValues();
        testSubscriber.assertError(HystrixRuntimeException.class);
        final Throwable actualError = testSubscriber.getOnErrorEvents().get(0).getCause();
        assertEquals(ClassCastException.class, actualError.getClass());
    }

    @Test
    public void shouldFailAndReceiveCustomExceptionFromCommand() throws Exception {
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        CommandExecutor<String> sut = CommandExecutors.webSocket(ofMain(COMMAND_CUSTOM_ERROR, MAIN_NODE, String.class));
        sut.execute("foo")
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
        CommandExecutor<String> sut = CommandExecutors.webSocket(ofMain(COMMAND_WITHOUT_ARGS, MAIN_NODE, String.class));
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        sut.execute(null)
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue("ok");
    }
}
