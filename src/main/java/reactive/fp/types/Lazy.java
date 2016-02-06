package reactive.fp.types;

import java.util.function.Supplier;

/**
 * @author Cipolinas on 2016.02.05.
 */
public final class Lazy<T> implements Supplier<T> {

    private final Object lock = new Object();
    private T value;
    private final Supplier<T> supplier;

    private Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public static <T> Lazy<T> of(final Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }

    @Override
    public T get() {
        synchronized (lock) {
            if (value == null) {
                value = supplier.get();
            }
            return value;
        }
    }
}
