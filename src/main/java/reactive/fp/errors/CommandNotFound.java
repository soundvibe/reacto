package reactive.fp.errors;

/**
 * @author Cipolinas on 2015.11.23.
 */
public class CommandNotFound extends RuntimeException {

    public CommandNotFound(String commandName) {
        super("Command not found: " + commandName);
    }
}
