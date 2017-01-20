package net.soundvibe.reacto.errors;

import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

/**
 * @author OZY on 2017.01.20.
 */
@SuppressWarnings("ThrowableNotThrown")
public class ReactiveExceptionTest {

    @Test
    public void shouldBeEqual() throws Exception {
        final ReactiveException sut = new ReactiveException(IllegalAccessException.class.getName(), "test", "stackTrace");
        final ReactiveException sut2 = new ReactiveException(IllegalAccessException.class.getName(), "test", "stackTrace");
        final ReactiveException sut3 = new ReactiveException(IllegalAccessException.class.getName(), "a", "stackTrace");

        assertEquals(sut, sut2);
        assertEquals(sut.hashCode(), sut2.hashCode());

        assertNotEquals(sut, sut3);
        assertNotEquals(sut.hashCode(), sut3.hashCode());

        assertTrue(sut.toString().startsWith("ReactiveException{"));
    }

    @Test
    public void shouldPrintStackTrace() throws Exception {
        final ReactiveException sut = new ReactiveException(IllegalAccessException.class.getName(), "test", "stackTrace");

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(outputStream);
        final PrintWriter printWriter = new PrintWriter(outputStream);
        sut.printStackTrace(printStream);

        final String actual = outputStream.toString();
        assertEquals("stackTrace" + System.lineSeparator(), actual);

        sut.printStackTrace(printWriter);
        final String actual2 = outputStream.toString();
        assertEquals("stackTrace" + System.lineSeparator(), actual2);

        printStream.close();
        printWriter.close();
    }

    @Test
    public void shouldGetMessage() throws Exception {
        final ReactiveException sut = new ReactiveException(IllegalAccessException.class.getName(), "test", "stackTrace");

        assertEquals("test", sut.getMessage());
    }
}