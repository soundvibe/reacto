package net.soundvibe.reacto.errors;

/**
 * @author Cipolinas on 2016.02.11.
 */
public class ConnectionClosedUnexpectedly extends RuntimeException {

    public ConnectionClosedUnexpectedly(String message) {
        super(message);
    }
}
