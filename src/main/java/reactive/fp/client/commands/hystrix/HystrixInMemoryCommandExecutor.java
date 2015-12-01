package reactive.fp.client.commands.hystrix;

import reactive.fp.client.commands.CommandExecutor;
import rx.Observable;

import java.util.function.Function;

/**
 * @author Cipolinas on 2015.12.01.
 */
public class HystrixInMemoryCommandExecutor<T> implements CommandExecutor<T> {

    private final String commandName;
    private final Function<Object, Observable<?>> f;

    public HystrixInMemoryCommandExecutor(String commandName, Function<Object, Observable<?>> f) {
        this.commandName = commandName;
        this.f = f;
    }

    @Override
    public Observable<T> execute(Object arg) {
        return new HystrixObservableCommandWrapper<T>(commandName, f, arg, 0).toObservable();
    }

    @Override
    public Observable<T> observe(Object arg, int executionTimeoutInMs) {
        return new HystrixObservableCommandWrapper<T>(commandName, f, arg, executionTimeoutInMs).toObservable();
    }

    @Override
    public String toString() {
        return "HystrixInMemoryCommandExecutor{" +
                "commandName='" + commandName + '\'' +
                ", f=" + f +
                '}';
    }
}
