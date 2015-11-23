package reactive.TestUtils;

import rx.Observer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Linas on 2015.11.03.
 */
public class RxTestSubscriber<T> implements Observer<T> {

    private final List<T> items = new ArrayList<>();
    private Throwable lastError = null;
    private boolean wasCompleted = false;

    public static <T> RxTestSubscriber<T> create() {
        return new RxTestSubscriber<>();
    }

    @Override
    public void onCompleted() {
        wasCompleted = true;
    }

    @Override
    public void onError(Throwable e) {
        lastError = e;
    }

    @Override
    public void onNext(T t) {
        items.add(t);
    }

    public boolean hasCompleted() {
        return wasCompleted;
    }

    public boolean hasCompletedWithError() {
        return lastError != null;
    }

    public int emittedItemCount() {
        return items.size();
    }

    public List<T> emittedItems() {
        return new ArrayList<>(items);
    }

    public Throwable getError() {
        return lastError;
    }

    public void assertValueCount(int count) {
        assertEquals(count, emittedItemCount());
    }

    public void assertValueCount(String message, int count) {
        assertEquals(message, count, emittedItemCount());
    }
}
