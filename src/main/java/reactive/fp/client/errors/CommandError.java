package reactive.fp.client.errors;

/**
 * @author OZY on 2015.12.11.
 */
public class CommandError extends RuntimeException {

    public CommandError(String message) {
        super(message);
    }
}
