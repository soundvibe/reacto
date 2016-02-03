package reactive.fp.client.commands.hystrix;

import com.netflix.hystrix.*;
import rx.Observable;

import java.util.function.Function;

/**
 * @author Cipolinas on 2015.12.01.
 */
public class HystrixObservableCommandWrapper<T, U> extends HystrixObservableCommand<U> {

    private final String commandName;
    private final Function<T, Observable<U>> f;
    private final T arg;

    public HystrixObservableCommandWrapper(String commandName, Function<T, Observable<U>> f, T arg, int executionTimeoutInMs) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("group: " + commandName))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withFallbackEnabled(false)
                        .withExecutionTimeoutEnabled(executionTimeoutInMs > 0)
                        .withExecutionTimeoutInMilliseconds(executionTimeoutInMs)
                )
                .andCommandKey(HystrixCommandKey.Factory.asKey(resolveCommandName(commandName, executionTimeoutInMs > 0))));
        this.commandName = commandName;
        this.f = f;
        this.arg = arg;
    }

    protected static String resolveCommandName(String name, boolean useExecutionTimeout) {
        return useExecutionTimeout ? name : name + "$";
    }

    @Override
    protected Observable<U> construct() {
        return f.apply(arg);
    }

    @Override
    public String toString() {
        return "HystrixObservableCommandWrapper{" +
                "commandName='" + commandName + '\'' +
                ", f=" + f +
                ", arg=" + arg +
                '}';
    }
}
