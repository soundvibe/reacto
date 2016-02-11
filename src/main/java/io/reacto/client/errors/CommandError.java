package io.reacto.client.errors;

/**
 * @author OZY on 2015.12.11.
 */
class CommandError extends RuntimeException {

    public CommandError(String message) {
        super(message);
    }
}
