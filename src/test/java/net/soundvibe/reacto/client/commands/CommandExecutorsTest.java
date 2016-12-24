package net.soundvibe.reacto.client.commands;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import io.vertx.core.Vertx;
import io.vertx.servicediscovery.*;
import io.vertx.servicediscovery.types.HttpEndpoint;
import net.soundvibe.reacto.client.errors.CannotDiscoverService;
import net.soundvibe.reacto.discovery.DiscoverableService;
import net.soundvibe.reacto.types.*;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

/**
 * @author Cipolinas on 2016.02.18.
 */
public class CommandExecutorsTest {

    private final TestSubscriber<Event> testSubscriber = new TestSubscriber<>();

    @Test
    public void shouldCreateWebSocketExecutor() throws Exception {
        final CommandExecutor sut = CommandExecutors.webSocket(Nodes.of("http://dummy/test"), 1000);
        sut.execute(Command.create("foo"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertError(HystrixRuntimeException.class);
    }

    @Test
    public void shouldCreateInMemoryExecutor() throws Exception {
        final CommandExecutor sut = CommandExecutors.inMemory(command -> Observable.empty(), 1000);
        sut.execute(Command.create("foo"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertNoValues();
    }

    @Test
    public void shouldCallFallbackWhenInMemoryMainExecutorFails() throws Exception {
        final CommandExecutor sut = CommandExecutors.inMemoryWithFallback(
                main -> Observable.error(new IllegalStateException("error")),
                fallback -> Observable.just(Event.create("ok")));
        sut.execute(Command.create("ok"))
                .subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(Event.create("ok"));
    }

    @Test
    public void shouldEmitErrorWhenFindIsUnableToGetServices() throws Exception {
        TestSubscriber<CommandExecutor> subscriber = new TestSubscriber<>();
        TestSubscriber<Record> recordTestSubscriber = new TestSubscriber<>();
        TestSubscriber<Record> closeSubscriber = new TestSubscriber<>();
        final ServiceDiscovery serviceDiscovery = ServiceDiscovery.create(Vertx.vertx());
        final DiscoverableService discoverableService = new DiscoverableService(serviceDiscovery);

        final Record record = HttpEndpoint.createRecord("testService", "localhost", 8123, "test/");
        discoverableService.startDiscovery(record)
                .subscribe(recordTestSubscriber);

        recordTestSubscriber.awaitTerminalEvent();
        recordTestSubscriber.assertNoErrors();

        discoverableService.closeDiscovery(record).subscribe(closeSubscriber);
        closeSubscriber.awaitTerminalEvent();
        closeSubscriber.assertNoErrors();

        CommandExecutors.find(Service.of("sdsd", serviceDiscovery))
                .subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        subscriber.assertError(CannotDiscoverService.class);
    }
}
