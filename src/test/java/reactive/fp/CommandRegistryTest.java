package reactive.fp;

import org.junit.Test;
import reactive.fp.server.CommandRegistry;
import rx.Observable;

import static org.junit.Assert.*;

/**
 * @author Linas on 2015.11.13.
 */
public class CommandRegistryTest {

    private final CommandRegistry sut = new CommandRegistry();

    @Test
    public void shouldFindCommand() throws Exception {
        sut.register("foo", o -> Observable.just(o + "bar"));

        assertTrue(sut.findCommand("foo").isPresent());
    }

    @Test
    public void shouldNotFindCommand() throws Exception {
        assertFalse(sut.findCommand("foobar").isPresent());
    }
}
