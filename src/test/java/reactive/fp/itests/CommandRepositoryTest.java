package reactive.fp.itests;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import reactive.fp.client.commands.CommandDef;
import reactive.fp.client.errors.CommandNotFound;
import reactive.fp.client.repositories.CommandRepository;
import reactive.fp.client.repositories.CommandRepositoryConfig;
import reactive.fp.server.CommandRegistry;
import reactive.fp.server.VertxServer;
import reactive.fp.server.WebServerConfig;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author OZY on 2015.11.13.
 */
public class CommandRepositoryTest {

    public static final String TEST_COMMAND = "test";
    public static final String TEST_COMMAND_MANY = "testMany";
    public static final String TEST_FAIL_COMMAND = "testFail";
    public static final String TEST_FAIL_BUT_FALLBACK_COMMAND = "testFailFallback";
    public static final String LONG_TASK = "longTask";
    public static final String MAIN_NODE = "http://localhost:8282/dist/";
    public static final String FALLBACK_NODE = "http://localhost:8383/distFallback/";

    private static final CommandRepositoryConfig config = CommandRepositoryConfig.from(
           CommandDef.ofMain(TEST_COMMAND, MAIN_NODE),
           CommandDef.ofMain(TEST_COMMAND_MANY, MAIN_NODE),
           CommandDef.ofMain(TEST_FAIL_COMMAND, MAIN_NODE),
           CommandDef.ofMain(LONG_TASK, MAIN_NODE),
           CommandDef.ofMainAndFallback(TEST_FAIL_BUT_FALLBACK_COMMAND, MAIN_NODE, FALLBACK_NODE)
    );


    private final CommandRepository sut = new CommandRepository(config);

    private static VertxServer vertxServer;
    private static VertxServer fallbackVertxServer;

    @BeforeClass
    public static void setUp() throws Exception {
        WebServerConfig webServerConfig = new WebServerConfig(8282, "dist/");
        CommandRegistry webCommandRegistry = CommandRegistry.of(TEST_COMMAND, o -> Observable.just("Called command with arg: " + o))
                .and(TEST_COMMAND_MANY, o -> Observable.just(
                        "1. Called command with arg: " + o,
                        "2. Called command with arg: " + o,
                        "3. Called command with arg: " + o
                ))
                .and(TEST_FAIL_COMMAND, o -> Observable.error(new RuntimeException("failed")))
                .and(TEST_FAIL_BUT_FALLBACK_COMMAND, o -> Observable.error(new RuntimeException("failed")))
                .and(LONG_TASK, (Integer interval) -> Observable.create(subscriber -> {
                    try {
                        Thread.sleep(interval);
                        subscriber.onNext("ok");
                        subscriber.onCompleted();
                    } catch (InterruptedException e) {
                        subscriber.onError(e);
                    }
                }))
                ;


        WebServerConfig distributedFallbackCommandsConfig = new WebServerConfig(8383, "distFallback/");
        CommandRegistry distributedFallbackCommandsRegistry = CommandRegistry.of(TEST_FAIL_BUT_FALLBACK_COMMAND, o -> Observable.just("Recovered: " + o));

        vertxServer = new VertxServer(webServerConfig, webCommandRegistry);
        fallbackVertxServer = new VertxServer(distributedFallbackCommandsConfig, distributedFallbackCommandsRegistry);
        fallbackVertxServer.start();
        vertxServer.start();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        vertxServer.stop();
        fallbackVertxServer.stop();
    }

    @Test
    public void shouldFindCommand() throws Exception {
        assertTrue(sut.findByName(TEST_COMMAND).isPresent());
    }

    @Test
    public void shouldNotFindCommand() throws Exception {
        assertFalse(sut.findByName("bla").isPresent());
    }

    @Test
    public void shouldGetCommand() throws Exception {
        assertNotNull(sut.getByName(TEST_COMMAND));
    }

    @Test(expected = CommandNotFound.class)
    public void shouldNotGetCommand() throws Exception {
        sut.getByName("foo");
    }

    @Test
    public void shouldExecuteCommand() throws Exception {
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        sut.getByName(TEST_COMMAND)
                .execute("foo", String.class)
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertValue("Called command with arg: foo");
    }

    @Test
    public void shouldCallCommandAndReceiveMultipleEvents() throws Exception {
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        sut.getByName(TEST_COMMAND_MANY)
                .execute("bar", String.class)
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
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
        sut.getByName(TEST_FAIL_COMMAND)
                .execute("foo", String.class)
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoValues();
        List<Throwable> errors = testSubscriber.getOnErrorEvents();
        assertEquals("Should be one error", 1, errors.size());
        Throwable throwable = errors.get(0);
        assertEquals("Error message should be failed", "testFail failed and fallback disabled.", throwable.getMessage());
        assertEquals("Should be HystrixRuntimeException", HystrixRuntimeException.class, throwable.getClass());
    }

    @Test
    public void shouldMainFailAndFallbackSucceed() throws Exception {
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        sut.getByName(TEST_FAIL_BUT_FALLBACK_COMMAND)
                .execute("foo", String.class)
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
        testSubscriber.assertValue("Recovered: foo");
    }

    @Test
    public void shouldFailWhenPayloadIsOfInvalidClass() throws Exception {
        TestSubscriber<Integer> testSubscriber = new TestSubscriber<>();
        sut.getByName(TEST_COMMAND)
                .execute("foo", Integer.class)
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertError(ClassCastException.class);
        testSubscriber.assertNotCompleted();
        testSubscriber.assertNoValues();
    }

    @Test
    public void shouldFailWhenCannotDeserializeObject() throws Exception {
        TestSubscriber<Foo> testSubscriber = new TestSubscriber<>();
        sut.getByName(TEST_COMMAND)
                .execute("bar", Foo.class)
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNotCompleted();
        testSubscriber.assertNoValues();
        testSubscriber.assertError(ClassCastException.class);
    }

    @Test
    public void shouldComposeDifferentCommands() throws Exception {
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        sut.getByName(TEST_COMMAND)
                .execute("foo", String.class)
                .mergeWith(sut.getByName(TEST_COMMAND_MANY)
                        .execute("bar", String.class)
                )
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
        sut.getByName(LONG_TASK)
                .observe(300000, String.class)
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNotCompleted();
        testSubscriber.assertNoValues();
        testSubscriber.assertError(HystrixRuntimeException.class);
    }

    private class Foo {
        public final String name;

        private Foo(String name) {
            this.name = name;
        }
    }
}

