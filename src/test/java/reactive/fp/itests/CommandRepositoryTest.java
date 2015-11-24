package reactive.fp.itests;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import reactive.fp.server.CommandRegistry;
import reactive.fp.client.repositories.CommandRepositoryConfig;
import reactive.fp.server.WebServerConfig;
import reactive.fp.client.errors.CommandNotFound;
import reactive.fp.client.repositories.CommandRepository;
import reactive.fp.client.commands.CommandNodes;
import reactive.fp.client.commands.DistributedCommandDef;
import reactive.fp.server.VertxServer;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author OZY on 2015.11.13.
 */
public class CommandRepositoryTest {

    public static final String TEST_COMMAND = "test";
    public static final String TEST_COMMAND_MANY = "testMany";
    public static final String TEST_FAIL_COMMAND = "testFail";
    public static final String TEST_FAIL_BUT_FALLBACK_COMMAND = "testFailFallback";

    private static final CommandRepositoryConfig config = CommandRepositoryConfig.create(
           new DistributedCommandDef(TEST_COMMAND, new CommandNodes("http://localhost:8282/dist/", Optional.empty())),
           new DistributedCommandDef(TEST_COMMAND_MANY, new CommandNodes("http://localhost:8282/dist/", Optional.empty())),
           new DistributedCommandDef(TEST_FAIL_COMMAND, new CommandNodes("http://localhost:8282/dist/", Optional.empty())),
           new DistributedCommandDef(TEST_FAIL_BUT_FALLBACK_COMMAND, new CommandNodes("http://localhost:8282/dist/", Optional.of("http://localhost:8383/distFallback/")))
    );

    private final CommandRepository sut = new CommandRepository(config);
    private static CommandRegistry webCommandRegistry;
    private static CommandRegistry distributedFallbackCommandsRegistry;

    private static VertxServer vertxServer;
    private static VertxServer fallbackVertxServer;

    @BeforeClass
    public static void setUp() throws Exception {
        WebServerConfig webServerConfig = new WebServerConfig(8282, "dist/");
        webCommandRegistry = new CommandRegistry()
                .register(TEST_COMMAND, o -> Observable.just("Called command with arg: " + o))
                .register(TEST_COMMAND_MANY, o -> Observable.just(
                    "1. Called command with arg: " + o,
                    "2. Called command with arg: " + o,
                    "3. Called command with arg: " + o
                    ))
                .register(TEST_FAIL_COMMAND, o -> Observable.error(new RuntimeException("failed")))
                .register(TEST_FAIL_BUT_FALLBACK_COMMAND, o -> Observable.error(new RuntimeException("failed")));


        WebServerConfig distributedFallbackCommandsConfig = new WebServerConfig(8383, "distFallback/");
        distributedFallbackCommandsRegistry = new CommandRegistry()
            .register(TEST_FAIL_BUT_FALLBACK_COMMAND, o -> Observable.just("Recovered: " + o));

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
        assertEquals("Error message should be failed", "testFail failed and no fallback available.", throwable.getMessage());
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

    private class Foo {
        public final String name;

        private Foo(String name) {
            this.name = name;
        }
    }
}

