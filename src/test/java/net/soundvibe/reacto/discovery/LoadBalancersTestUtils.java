package net.soundvibe.reacto.discovery;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author OZY on 2016.08.26.
 */
public interface LoadBalancersTestUtils {

    Name record1 = new Name("test", "localhost/1");
    Name record2 = new Name("test", "localhost/2");
    Name record3 = new Name("test", "localhost/3");
    Name record4 = new Name("test", "localhost/4");
    Name record5 = new Name("test", "localhost/5");

    static void assertRecords(Name expected, Name actual, String message) {
        assertEquals(message, expected, actual);
    }

    static void assertOneOf(Name expected, List<Name> ofRecords) {
        assertTrue("None of " + ofRecords + " matches " + expected, ofRecords.stream()
                .anyMatch(expected::equals));
    }

}
