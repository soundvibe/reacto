package net.soundvibe.reacto.errors;

/**
 * @author OZY on 2016.07.15.
 */
public class CannotDiscoverService extends RuntimeException {

    public CannotDiscoverService(String message) {
        super(message);
    }

    public CannotDiscoverService(String message, Throwable cause) {
        super(message, cause);
    }
}
