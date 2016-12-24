package net.soundvibe.reacto.discovery;

import org.junit.Test;

import java.util.*;

import static net.soundvibe.reacto.discovery.LoadBalancersTestUtils.*;

/**
 * @author OZY on 2016.08.26.
 */
public class RoundRobinLoadBalancerTest {

    private final LoadBalancer<Name> sut = new RoundRobinLoadBalancer<>();

    @Test
    public void shouldBalanceWhenRecordsDoNotChange() throws Exception {
        final List<Name> records = Arrays.asList(record1, record2, record3, record4, record5);

        final Name actual1 = sut.balance(records);
        assertRecords(record1, actual1, "1");

        final Name actual2 = sut.balance(records);
        assertRecords(record2, actual2,"2");

        final Name actual3 = sut.balance(records);
        assertRecords(record3, actual3,"3");

        final Name actual4 = sut.balance(records);
        assertRecords(record4, actual4,"4");

        final Name actual5 = sut.balance(records);
        assertRecords(record5, actual5,"5");

        final Name actual6 = sut.balance(records);
        assertRecords(record1, actual6,"6");
    }

    @Test
    public void shouldBalanceWhenRecordsAreChanging() throws Exception {
        final List<Name> records = Arrays.asList(record1, record2, record3, record4, record5);
        final Name actual1 = sut.balance(records);
        assertRecords(record1, actual1,"1");

        final Name actual2 = sut.balance(records);
        assertRecords(record2, actual2,"2");

        final Name actual3 = sut.balance(records);
        assertRecords(record3, actual3,"3");

        final List<Name> newRecords = Arrays.asList(record1, record2);

        final Name newRecord1 = sut.balance(newRecords);
        assertRecords(record1, newRecord1,"new1");

        final Name newRecord2 = sut.balance(newRecords);
        assertRecords(record2, newRecord2, "new2");

        final Name newRecord = sut.balance(newRecords);
        assertRecords(record1, newRecord,"new3");
    }
}