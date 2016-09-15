package net.soundvibe.reacto.discovery;

import io.vertx.servicediscovery.Record;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static net.soundvibe.reacto.discovery.LoadBalancersTestUtils.*;

/**
 * @author OZY on 2016.08.26.
 */
public class RandomLoadBalancerTest {

    private final LoadBalancer sut = new RandomLoadBalancer();

    @Test
    public void shouldBalance() throws Exception {
        final List<Record> records = Arrays.asList(record1, record2, record3, record4, record5);

        final Record actual1 = sut.balance(records);
        assertOneOf(actual1, records);

        final Record actual2 = sut.balance(records);
        assertOneOf(actual2, records);

        final Record actual3 = sut.balance(records);
        assertOneOf(actual3, records);

        final Record actual4 = sut.balance(records);
        assertOneOf(actual4, records);

        final Record actual5 = sut.balance(records);
        assertOneOf(actual5, records);
    }
}