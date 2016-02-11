package net.soundvibe.reacto.client.commands.hystrix;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import net.soundvibe.reacto.types.Event;
import net.soundvibe.reacto.types.Command;
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
