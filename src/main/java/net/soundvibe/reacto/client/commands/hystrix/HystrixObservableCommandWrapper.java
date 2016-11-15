package net.soundvibe.reacto.client.commands.hystrix;

import com.netflix.hystrix.*;
import io.vertx.core.logging.*;
import net.soundvibe.reacto.types.*;
import rx.Observable;

import java.util.Optional;
import java.util.function.Function;

/**
 * @author Cipolinas on 2015.12.01.
 */
public class HystrixObservableCommandWrapper extends HystrixObservableCommand<Event> {

    private static final Logger log = LoggerFactory.getLogger(HystrixObservableCommandWrapper.class);

    private final Function<Command, Observable<Event>> main;
    private final Optional<Function<Command, Observable<Event>>> fallback;
    private final Command command;

    public HystrixObservableCommandWrapper(Function<Command, Observable<Event>> main, Command command, HystrixCommandProperties.Setter hystrixConfig) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("reacto"))
                .andCommandPropertiesDefaults(hystrixConfig
                        .withFallbackEnabled(false))
                .andCommandKey(HystrixCommandKey.Factory.asKey(resolveCommandName(command.name, hystrixConfig.getExecutionTimeoutEnabled()))));
        this.main = main;
        this.fallback = Optional.empty();
        this.command = command;
    }

    public HystrixObservableCommandWrapper(Function<Command, Observable<Event>> main, Function<Command, Observable<Event>> fallback,
                                           Command command, HystrixCommandProperties.Setter hystrixConfig) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("reacto"))
                .andCommandPropertiesDefaults(hystrixConfig
                        .withFallbackEnabled(true)
                )
                .andCommandKey(HystrixCommandKey.Factory.asKey(resolveCommandName(command.name, hystrixConfig.getExecutionTimeoutEnabled()))));
        this.main = main;
        this.fallback = Optional.of(fallback);
        this.command = command;
    }

    private static String resolveCommandName(String name, boolean useExecutionTimeout) {
        return useExecutionTimeout ? name : name + "$";
    }

    @Override
    protected Observable<Event> construct() {
        log.debug("Command executed: " + command);
        return main.apply(command);
    }

    @Override
    protected Observable<Event> resumeWithFallback() {
        log.debug("Resuming with fallback: " + command);
        return fallback.map(f -> f.apply(command)).orElseGet(() -> super.resumeWithFallback());
    }
}
