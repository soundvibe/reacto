package net.soundvibe.reacto.client.commands;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Cipolinas on 2016.02.18.
 */
public class NodesTest {

    @Test
    public void shouldPrintToStringCorrectly() throws Exception {
        final Nodes sut = Nodes.ofMain("localhost");
        final String actual = sut.toString();
        final String expected = "Nodes{mainNode='localhost', fallbackNode=Optional.empty}";
        assertEquals(expected, actual);
    }

    @Test
    public void shouldBeAbleToFindInMaps() throws Exception {
        Map<Nodes, Boolean> nodes = new HashMap<>();
        nodes.put(Nodes.ofMain("localhost"), true);
        nodes.put(Nodes.ofMain("server"), true);
        nodes.put(Nodes.ofMainAndFallback("local", "someServer"), true);

        assertFalse(nodes.containsKey(Nodes.ofMain("foo")));
        assertTrue(nodes.containsKey(Nodes.ofMain("server")));
        assertTrue(nodes.containsKey(Nodes.ofMain("localhost")));
        assertTrue(nodes.containsKey(Nodes.ofMainAndFallback("local", "someServer")));
    }
}
