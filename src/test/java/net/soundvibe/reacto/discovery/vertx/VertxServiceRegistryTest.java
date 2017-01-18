package net.soundvibe.reacto.discovery.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.json.*;
import io.vertx.servicediscovery.*;
import io.vertx.servicediscovery.types.HttpEndpoint;
import net.soundvibe.reacto.client.commands.*;
import net.soundvibe.reacto.client.errors.CannotDiscoverService;
import net.soundvibe.reacto.discovery.types.ServiceRecord;
import net.soundvibe.reacto.mappers.jackson.JacksonMapper;
import net.soundvibe.reacto.server.CommandRegistry;
import net.soundvibe.reacto.server.vertx.Service;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.utils.*;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

/**
 * @author linas on 17.1.9.
 */
public class VertxServiceRegistryTest {

    private static final String TEST_SERVICE = "testService";
    private static final String ROOT = "/test/";

    private final Vertx vertx = Vertx.vertx();
    private final ServiceDiscovery serviceDiscovery = ServiceDiscovery.create(vertx);

    private final VertxServiceRegistry sut = new VertxServiceRegistry(serviceDiscovery, new DemoServiceRegistryMapper());

    @Test
    public void shouldStartDiscovery() throws Exception {
        assertDiscoveredServices(0);

        TestSubscriber<Any> recordTestSubscriber = new TestSubscriber<>();

        final ServiceRecord httpEndpoint = ServiceRecord.createHttpEndpoint(TEST_SERVICE, 8181, ROOT, "0.1");

        sut.startDiscovery(httpEndpoint, CommandRegistry.empty())
                .subscribe(recordTestSubscriber);

        recordTestSubscriber.awaitTerminalEvent();
        recordTestSubscriber.assertNoErrors();
        recordTestSubscriber.assertValueCount(1);

        assertDiscoveredServices(1);
    }

    @Test
    public void shouldCloseDiscovery() throws Exception {
        shouldStartDiscovery();
        TestSubscriber<Any> closeSubscriber = new TestSubscriber<>();
        sut.closeDiscovery().subscribe(closeSubscriber);
        closeSubscriber.awaitTerminalEvent();
        closeSubscriber.assertNoErrors();
        closeSubscriber.assertValueCount(1);

        assertDiscoveredServices(0);
    }

    @Test
    public void shouldRemoveDownRecords() throws Exception {
        shouldStartDiscovery();

        final Record record = HttpEndpoint.createRecord(
                TEST_SERVICE,
                WebUtils.getLocalAddress(),
                8888,
                ROOT);

        serviceDiscovery.publish(record, event -> {});
        Thread.sleep(100L);
        serviceDiscovery.update(record.setStatus(Status.DOWN), event -> {});
        Thread.sleep(100L);

        List<Record> recordList = getRecords(Status.DOWN);
        assertEquals("Should be one service down", 1, recordList.size());
        TestSubscriber<Record> testSubscriber = new TestSubscriber<>();

        sut.cleanServices().subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValueCount(1);

        List<Record> records = getRecords(Status.DOWN);
        assertEquals("Should be no services down", 0, records.size());
    }

    @Test
    public void shouldSerializeCommandsToJson() throws Exception {
        CommandRegistry commandRegistry = CommandRegistry
                .of("foo", command -> rx.Observable.empty())
                .and("bar", command -> rx.Observable.empty());

        final JsonArray array = VertxServiceRegistry.commandsToJsonArray(commandRegistry);
        final JsonObject jsonObject = new JsonObject().put("commands", array);
        final String actual = jsonObject.encode();
        final String expected1 = "{\"commands\":[{\"commandType\":\"bar\",\"eventType\":\"\"},{\"commandType\":\"foo\",\"eventType\":\"\"}]}";
        final String expected2 = "{\"commands\":[{\"commandType\":\"foo\",\"eventType\":\"\"},{\"commandType\":\"bar\",\"eventType\":\"\"}]}";
        assertTrue("Was not " + expected1 + " or " + expected2 + " but was " + actual,
                actual.equals(expected1) || actual.equals(expected2));
    }

    @Test
    public void shouldSerializeTypedCommandToJson() throws Exception {
        CommandRegistry commandRegistry = CommandRegistry
                .ofTyped(MakeDemo.class, DemoMade.class, makeDemo -> rx.Observable.empty(), new DemoCommandRegistryMapper());

        final JsonArray array = VertxServiceRegistry.commandsToJsonArray(commandRegistry);
        final JsonObject jsonObject = new JsonObject().put("commands", array);
        final String actual = jsonObject.encode();
        final String expected = "{\"commands\":[{\"commandType\":\"net.soundvibe.reacto.types.MakeDemo\",\"eventType\":\"net.soundvibe.reacto.types.DemoMade\"}]}";
        assertEquals(expected, actual);
    }


    @Test
    public void shouldEmitErrorWhenFindIsUnableToGetServices() throws Exception {
        TestSubscriber<CommandExecutor> subscriber = new TestSubscriber<>();
        TestSubscriber<Any> recordTestSubscriber = new TestSubscriber<>();
        TestSubscriber<Any> closeSubscriber = new TestSubscriber<>();
        final ServiceDiscovery serviceDiscovery = ServiceDiscovery.create(Vertx.vertx());
        final VertxServiceRegistry lifecycle = new VertxServiceRegistry(serviceDiscovery,
                new JacksonMapper(Json.mapper));

        final ServiceRecord record = ServiceRecord.createHttpEndpoint("testService", 8123, "test/", "0.1");
        lifecycle.startDiscovery(record, CommandRegistry.empty())
                .subscribe(recordTestSubscriber);

        recordTestSubscriber.awaitTerminalEvent();
        recordTestSubscriber.assertNoErrors();

        lifecycle.closeDiscovery().subscribe(closeSubscriber);
        closeSubscriber.awaitTerminalEvent();
        closeSubscriber.assertNoErrors();

        CommandExecutors.find(Service.of("sdsd", serviceDiscovery))
                .subscribe(subscriber);

        subscriber.awaitTerminalEvent();
        subscriber.assertError(CannotDiscoverService.class);
    }

    private List<Record> getRecords(Status status) throws InterruptedException {
        List<Record> recordList = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        serviceDiscovery.getRecords(record -> record.getStatus().equals(status), true, event -> {
            if (event.succeeded()) {
                recordList.addAll(event.result());
            }
            countDownLatch.countDown();
        });
        countDownLatch.await();
        return recordList;
    }

    private void assertDiscoveredServices(int count) {
        TestSubscriber<CommandExecutor> testSubscriber = new TestSubscriber<>();

        DiscoverableServices.find(TEST_SERVICE, serviceDiscovery)
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();

        if (count > 0) {
            testSubscriber.assertNoErrors();
            testSubscriber.assertValueCount(count);
        } else {
            testSubscriber.assertError(CannotDiscoverService.class);
        }
    }

}