package reactive.TestUtils.models;

/**
 * @author OZY on 2015.12.11.
 */
public class CustomError extends RuntimeException {

    public final String data;

    public CustomError(String data) {
        this.data = data;
    }

}
