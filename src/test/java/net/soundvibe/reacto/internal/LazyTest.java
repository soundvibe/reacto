package net.soundvibe.reacto.internal;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Cipolinas on 2016.02.18.
 */
public class LazyTest {

    @Test
    public void shouldBeAbleToFindInSet() throws Exception {
        Set<Lazy<String>> lazySet = new HashSet<>();
        lazySet.add(Lazy.of(() -> "foo"));
        lazySet.add(Lazy.of(() -> "bar"));
        lazySet.add(Lazy.of(() -> ""));

        assertTrue("foo not found", lazySet.contains(Lazy.of(() -> "foo")));
        assertTrue("bar not found", lazySet.contains(Lazy.of(() -> "bar")));
        assertFalse("bla found", lazySet.contains(Lazy.of(() -> "bla")));
    }
}
