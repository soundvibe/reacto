package net.soundvibe.reacto.discovery;

import io.vertx.core.Vertx;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.Status;
import io.vertx.servicediscovery.types.HttpEndpoint;
import net.soundvibe.reacto.client.commands.CommandExecutor;
import net.soundvibe.reacto.client.errors.CannotDiscoverService;
import net.soundvibe.reacto.utils.WebUtils;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;

/**
 * @author OZY on 2016.08.28.
 */
public class DiscoverableServicesTest {

    private static final String TEST_SERVICE = "testService";
    private static final String ROOT = "/test/";
    private final Vertx vertx = Vertx.vertx();
    private final ServiceDiscovery serviceDiscovery = ServiceDiscovery.create(vertx);
    private final DiscoverableService sut = new DiscoverableService(serviceDiscovery);


    @Test
    public void shouldStartDiscovery() throws Exception {
        assertDiscoveredServices(0);

        TestSubscriber<Record> recordTestSubscriber = new TestSubscriber<>();
        final Record record = HttpEndpoint.createRecord(
                TEST_SERVICE,
                WebUtils.getLocalAddress(),
                8181,
                ROOT);

        sut.startDiscovery(record)
            .subscribe(recordTestSubscriber);

        recordTestSubscriber.awaitTerminalEvent();
        recordTestSubscriber.assertNoErrors();
        recordTestSubscriber.assertValue(record);

        assertDiscoveredServices(1);
    }

    @Test
    public void shouldUnsubscribe() throws Exception {
        TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        Observable<String> observable = Observable.create(subscriber -> {
            for (int i = 0; i < 5; i++) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext("ok");
                    subscriber.onCompleted();
                }
            }
        });
        observable.subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
        testSubscriber.assertValueCount(1);
        testSubscriber.assertValue("ok");
    }

    @Test
    public void shouldCloseDiscovery() throws Exception {
        shouldStartDiscovery();

        final Record record = HttpEndpoint.createRecord(
                TEST_SERVICE,
                WebUtils.getLocalAddress(),
                8181,
                ROOT);

        TestSubscriber<Record> closeSubscriber = new TestSubscriber<>();
        sut.closeDiscovery(record).subscribe(closeSubscriber);
        closeSubscriber.awaitTerminalEvent();
        closeSubscriber.assertNoErrors();
        closeSubscriber.assertValueCount(1);

        assertDiscoveredServices(0);
    }

    @Test
    public void shouldRemoveDownRecords() throws Exception {
        shouldCloseDiscovery();

        final Record record = HttpEndpoint.createRecord(
                TEST_SERVICE,
                WebUtils.getLocalAddress(),
                8181,
                ROOT);

        serviceDiscovery.publish(record, event -> {});
        Thread.sleep(100L);
        serviceDiscovery.update(record.setStatus(Status.DOWN), event -> {});
        Thread.sleep(100L);

        List<Record> recordList = getRecords(Status.DOWN);
        assertEquals("Should be one service down", 1, recordList.size());
        TestSubscriber<Record> testSubscriber = new TestSubscriber<>();

        sut.removeRecordsWithStatus(Status.DOWN)
                .subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValueCount(1);

        List<Record> records = getRecords(Status.DOWN);
        assertEquals("Should be no services down", 0, records.size());
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

        sut.find(TEST_SERVICE)
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