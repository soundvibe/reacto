package net.soundvibe.reacto.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.soundvibe.reacto.errors.CannotDiscoverService;
import net.soundvibe.reacto.mappers.jackson.*;
import net.soundvibe.reacto.server.CommandRegistry;
import net.soundvibe.reacto.types.*;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

/**
 * @author OZY on 2017.01.25.
 */
public class LocalServiceRegistryTest {

    private final static ObjectMapper json = new ObjectMapper();
    private final static JacksonMapper jacksonMapper = new JacksonMapper(json);

    @Test
    public void shouldExecuteSuccessfully() throws Exception {
        LocalServiceRegistry sut = new LocalServiceRegistry(
                jacksonMapper,
                CommandRegistry.ofTyped(JacksonCommand.class, JacksonEvent.class,
                        cmd -> Observable.just(new JacksonEvent(cmd.name)),
                jacksonMapper));


        TestSubscriber<JacksonEvent> testSubscriber = new TestSubscriber<>();
        sut.execute(new JacksonCommand("foo"), JacksonEvent.class)
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(new JacksonEvent("foo"));
    }

    @Test
    public void shouldExecuteDefaultCommandSuccessfully() throws Exception {
        LocalServiceRegistry sut = new LocalServiceRegistry(
                jacksonMapper,
                CommandRegistry.of("foo", cmd -> Observable.just(Event.create("foo"))));


        TestSubscriber<Event> testSubscriber = new TestSubscriber<>();
        sut.execute(Command.create("foo"), Event.class)
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(Event.create("foo"));
    }

    @Test
    public void shouldNotFindService() throws Exception {
        LocalServiceRegistry sut = new LocalServiceRegistry(
                jacksonMapper,
                CommandRegistry.ofTyped(JacksonCommand.class, DemoMade.class,
                        cmd -> Observable.just(new DemoMade(cmd.name)),
                        jacksonMapper));


        TestSubscriber<JacksonEvent> testSubscriber = new TestSubscriber<>();
        sut.execute(new JacksonCommand("foo"), JacksonEvent.class)
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNotCompleted();
        testSubscriber.assertError(CannotDiscoverService.class);
    }

    @Test
    public void shouldRegisterSuccessfully() throws Exception {
        LocalServiceRegistry sut = new LocalServiceRegistry(
                jacksonMapper, CommandRegistry.empty());

        TestSubscriber<Any> testSubscriber = new TestSubscriber<>();
        sut.register()
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
        testSubscriber.assertValue(Any.VOID);
    }

    @Test
    public void shouldTryToRegisterWhenAlreadyRegistered() throws Exception {
        LocalServiceRegistry sut = new LocalServiceRegistry(
                jacksonMapper, CommandRegistry.empty());

        TestSubscriber<Any> testSubscriber = new TestSubscriber<>();
        sut.register()
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
        testSubscriber.assertValue(Any.VOID);

        TestSubscriber<Any> testSubscriber2 = new TestSubscriber<>();
        sut.register()
                .subscribe(testSubscriber2);

        testSubscriber2.awaitTerminalEvent();
        testSubscriber2.assertNoValues();
        testSubscriber2.assertCompleted();
    }

    @Test
    public void shouldUnregisterSuccessfully() throws Exception {
        LocalServiceRegistry sut = new LocalServiceRegistry(
                jacksonMapper, CommandRegistry.empty());

        TestSubscriber<Any> testSubscriber = new TestSubscriber<>();
        sut.register()
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
        testSubscriber.assertValue(Any.VOID);

        TestSubscriber<Any> testSubscriber2 = new TestSubscriber<>();
        sut.unregister()
                .subscribe(testSubscriber2);

        testSubscriber2.awaitTerminalEvent();
        testSubscriber2.assertNoErrors();
        testSubscriber2.assertCompleted();
        testSubscriber2.assertValue(Any.VOID);
    }

    @Test
    public void shouldTryToUnregisterWhenAlreadyUnregistered() throws Exception {
        LocalServiceRegistry sut = new LocalServiceRegistry(
                jacksonMapper, CommandRegistry.empty());

        TestSubscriber<Any> testSubscriber = new TestSubscriber<>();
        sut.unregister()
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoValues();
    }
}