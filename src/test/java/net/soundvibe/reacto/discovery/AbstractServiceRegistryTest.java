package net.soundvibe.reacto.discovery;

import com.codahale.metrics.ConsoleReporter;
import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;
import net.soundvibe.reacto.client.commands.*;
import net.soundvibe.reacto.client.events.*;
import net.soundvibe.reacto.discovery.types.*;
import net.soundvibe.reacto.errors.CannotFindEventHandlers;
import net.soundvibe.reacto.mappers.ServiceRegistryMapper;
import net.soundvibe.reacto.mappers.jackson.JacksonMapper;
import net.soundvibe.reacto.metric.Metrics;
import net.soundvibe.reacto.server.*;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.types.json.JsonObject;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * @author OZY on 2017.01.19.
 */
public class AbstractServiceRegistryTest {

    @Test
    public void shouldNotFindAnyEventHandlers() throws Exception {
        CommandHandlerRegistry registry = CommandHandlerRegistry.empty();
        ServiceRegistryMapper mapper = new ServiceRegistryMapper() {
            @Override
            public <C, E> TypedCommand toCommand(C genericCommand, Class<? extends E> eventClass) {
                return null;
            }

            @Override
            public <E> E toGenericEvent(Event event, Class<? extends E> eventClass) {
                return null;
            }
        };
        TestServiceRegistry sut = new TestServiceRegistry(registry, mapper);

        TestSubscriber<CommandExecutor> testSubscriber = new TestSubscriber<>();
        ServiceOptions serviceOptions = new ServiceOptions(
                "test", "/", "1", false, 8080
        );
        sut.findExecutor(Flowable.just(Collections.singletonList(
                ServiceRecord.createWebSocketEndpoint(serviceOptions, Collections.emptyList()))),
                "foo", LoadBalancers.ROUND_ROBIN,
                ReactoCommandExecutor.FACTORY)
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertError(CannotFindEventHandlers.class);
        testSubscriber.assertNotComplete();
    }

    @Test
    public void shouldExecuteWithMetrics() throws Exception {
        final ConsoleReporter reporter = ConsoleReporter.forRegistry(Metrics.REGISTRY)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();

        final CommandRegistry commandRegistry = CommandRegistry.of("simple",
                command -> Flowable.just(Event.create("one"), Event.create("two"), Event.create("three")));

        TestServiceRegistry sut = new TestServiceRegistry(CommandHandlerRegistry.Builder.create()
                .register(ServiceType.LOCAL, serviceRecord -> new LocalCommandHandler(serviceRecord, commandRegistry))
                .build(),
                new JacksonMapper(JacksonMapper.JSON));

        Stream.of(new TestSubscriber<Event>(), new TestSubscriber<Event>(), new TestSubscriber<Event>(), new TestSubscriber<Event>())
                .parallel()
                .forEach(eventTestSubscriber -> {
                    sut.execute(Command.create("simple"))
                            .subscribe(eventTestSubscriber);

                            eventTestSubscriber.awaitTerminalEvent();
                            eventTestSubscriber.assertNoErrors();
                            eventTestSubscriber.assertComplete();
                            eventTestSubscriber.assertValueCount(3);
                });
        reporter.report();
    }

    public class TestServiceRegistry extends AbstractServiceRegistry {

        TestServiceRegistry(CommandHandlerRegistry commandHandlerRegistry, ServiceRegistryMapper mapper) {
            super(commandHandlerRegistry, mapper);
        }

        @Override
        protected Flowable<List<ServiceRecord>> findRecordsOf(Command command) {
            return Flowable.just(Collections.singletonList(ServiceRecord.create("test", Status.UP, ServiceType.LOCAL,
                    "111", JsonObject.empty(), JsonObject.empty())));
        }

        @Override
        public Flowable<Any> register() {
            return Flowable.just(Any.VOID);
        }

        @Override
        public Flowable<Any> unregister() {
            return Flowable.just(Any.VOID);
        }
    }
}