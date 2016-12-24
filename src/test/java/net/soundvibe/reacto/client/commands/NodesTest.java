package net.soundvibe.reacto.client.commands;

import net.soundvibe.reacto.types.Nodes;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Cipolinas on 2016.02.18.
 */
public class NodesTest {

    @Test
    public void shouldPrintToStringCorrectly() throws Exception {
        final Nodes sut = Nodes.of("localhost");
        final String actual = sut.toString();
        final String expected = "Nodes{nodes=[localhost/]}";
        assertEquals(expected, actual);
    }

    @Test
    public void shouldBeAbleToFindInMaps() throws Exception {
        Map<Nodes, Boolean> nodes = new HashMap<>();
        nodes.put(Nodes.of("localhost"), true);
        nodes.put(Nodes.of("server"), true);
        nodes.put(Nodes.of("local", "someServer"), true);

        assertFalse(nodes.containsKey(Nodes.of("foo")));
        assertTrue(nodes.containsKey(Nodes.of("server")));
        assertTrue(nodes.containsKey(Nodes.of("localhost")));
        assertTrue(nodes.containsKey(Nodes.of("local", "someServer")));
    }
}
