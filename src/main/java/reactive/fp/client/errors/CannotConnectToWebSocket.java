package reactive.fp.client.errors;

/**
 * @author Cipolinas on 2016.02.05.
 */
public class CannotConnectToWebSocket extends RuntimeException {

    public CannotConnectToWebSocket(String message) {
        super(message);
    }
}
