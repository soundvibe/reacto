package net.soundvibe.reacto.discovery;

import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocketStream;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;
import net.soundvibe.reacto.utils.WebUtils;
import org.junit.Test;
import rx.observers.TestSubscriber;

import static org.junit.Assert.*;

/**
 * @author OZY on 2016.08.28.
 */
public class DiscoverableServicesTest {

    private static final String TEST_SERVICE = "testService";
    private static final String ROOT = "/test/";
    private final Vertx vertx = Vertx.vertx();
    private final ServiceDiscovery sut = ServiceDiscovery.create(vertx);

    @Test
    public void shouldStartDiscovery() throws Exception {
        TestSubscriber<Record> recordTestSubscriber = new TestSubscriber<>();
        final Record record = HttpEndpoint.createRecord(
                TEST_SERVICE,
                WebUtils.getLocalAddress(),
                8181,
                ROOT);

        DiscoverableServices.startDiscovery(sut, record)
            .subscribe(recordTestSubscriber);

        recordTestSubscriber.awaitTerminalEvent();
        recordTestSubscriber.assertNoErrors();
        recordTestSubscriber.assertValue(record);

        TestSubscriber<WebSocketStream> testSubscriber = new TestSubscriber<>();

        DiscoverableServices.find(TEST_SERVICE, sut, LoadBalancers.ROUND_ROBIN)
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValueCount(1);
    }

    @Test
    public void shouldCloseDiscovery() throws Exception {
        TestSubscriber<WebSocketStream> testSubscriber = new TestSubscriber<>();
        final Record record = HttpEndpoint.createRecord(
                TEST_SERVICE,
                WebUtils.getLocalAddress(),
                8181,
                ROOT);
        DiscoverableServices.closeDiscovery(sut, record);
        DiscoverableServices.find(TEST_SERVICE, sut, LoadBalancers.ROUND_ROBIN)
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertNoValues();
    }
}