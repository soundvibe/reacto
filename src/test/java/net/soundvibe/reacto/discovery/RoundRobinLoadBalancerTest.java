package net.soundvibe.reacto.discovery;

import io.vertx.servicediscovery.Record;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static net.soundvibe.reacto.discovery.LoadBalancersTestUtils.*;

/**
 * @author OZY on 2016.08.26.
 */
public class RoundRobinLoadBalancerTest {

    private final LoadBalancer sut = new RoundRobinLoadBalancer();

    @Test
    public void shouldBalanceWhenRecordsDoNotChange() throws Exception {
        final List<Record> records = Arrays.asList(record1, record2, record3, record4, record5);

        final Record actual1 = sut.balance(records);
        assertRecords(record1, actual1);

        final Record actual2 = sut.balance(records);
        assertRecords(record2, actual2);

        final Record actual3 = sut.balance(records);
        assertRecords(record3, actual3);

        final Record actual4 = sut.balance(records);
        assertRecords(record4, actual4);

        final Record actual5 = sut.balance(records);
        assertRecords(record5, actual5);

        final Record actual6 = sut.balance(records);
        assertRecords(record1, actual6);
    }

    @Test
    public void shouldBalanceWhenRecordsAreChanging() throws Exception {
        final List<Record> records = Arrays.asList(record1, record2, record3, record4, record5);
        final Record actual1 = sut.balance(records);
        assertRecords(record1, actual1);

        final Record actual2 = sut.balance(records);
        assertRecords(record2, actual2);

        final Record actual3 = sut.balance(records);
        assertRecords(record3, actual3);

        final List<Record> newRecords = Arrays.asList(record1, record2);

        final Record newRecord1 = sut.balance(newRecords);
        assertRecords(record1, newRecord1);

        final Record newRecord2 = sut.balance(newRecords);
        assertRecords(record2, newRecord2);

        final Record newRecord = sut.balance(newRecords);
        assertRecords(record1, newRecord);
    }
}