package net.soundvibe.reacto.discovery;

import org.junit.Test;

import java.util.*;

import static net.soundvibe.reacto.discovery.LoadBalancersTestUtils.*;

/**
 * @author OZY on 2016.08.26.
 */
public class RandomLoadBalancerTest {

    private final LoadBalancer<Name> sut = new RandomLoadBalancer<>();

    @Test
    public void shouldBalance() throws Exception {
        final List<Name> records = Arrays.asList(record1, record2, record3, record4, record5);

        final Name actual1 = sut.balance(records);
        assertOneOf(actual1, records);

        final Name actual2 = sut.balance(records);
        assertOneOf(actual2, records);

        final Name actual3 = sut.balance(records);
        assertOneOf(actual3, records);

        final Name actual4 = sut.balance(records);
        assertOneOf(actual4, records);

        final Name actual5 = sut.balance(records);
        assertOneOf(actual5, records);
    }
}