package reactive.fp.client.commands.hystrix;

import com.netflix.hystrix.*;
import reactive.fp.types.Command;
import reactive.fp.types.Event;
import rx.Observable;

import java.util.function.Function;

/**
 * @author Cipolinas on 2015.12.01.
 */
public class HystrixObservableCommandWrapper extends HystrixObservableCommand<Event> {

    private final Function<Command, Observable<Event>> f;
    private final Command command;

    public HystrixObservableCommandWrapper(Function<Command, Observable<Event>> f, Command command, int executionTimeoutInMs) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("group: " + command.name))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withFallbackEnabled(false)
                        .withExecutionTimeoutEnabled(executionTimeoutInMs > 0)
                        .withExecutionTimeoutInMilliseconds(executionTimeoutInMs)
                )
                .andCommandKey(HystrixCommandKey.Factory.asKey(resolveCommandName(command.name, executionTimeoutInMs > 0))));
        this.f = f;
        this.command = command;
    }

    protected static String resolveCommandName(String name, boolean useExecutionTimeout) {
        return useExecutionTimeout ? name : name + "$";
    }

    @Override
    protected Observable<Event> construct() {
        return f.apply(command);
    }

    @Override
    public String toString() {
        return "HystrixObservableCommandWrapper{" +
                "f=" + f +
                ", command=" + command +
                "} " + super.toString();
    }
}
