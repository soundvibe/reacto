package net.soundvibe.reacto.metric;

import net.soundvibe.reacto.types.Command;
import net.soundvibe.reacto.utils.Exceptions;

import java.util.Optional;
import java.util.concurrent.atomic.*;

/**
 * @author OZY on 2017.01.12.
 */
public final class CommandHandlerMetric {

    private final String commandName;
    private final String eventName;
    private AtomicInteger onNextCount;
    private AtomicBoolean hasCompleted;
    private AtomicReference<String> hasError;
    private final long timeStarted;
    private AtomicLong timeElapsedInMs;

    private CommandHandlerMetric(String commandName, String eventName) {
        this.commandName = commandName;
        this.eventName = eventName;
        this.onNextCount = new AtomicInteger(0);
        this.hasCompleted = new AtomicBoolean(false);
        this.hasError = new AtomicReference<>(null);
        this.timeElapsedInMs = new AtomicLong(0L);
        this.timeStarted = System.nanoTime();
    }

    public static CommandHandlerMetric of(Command command) {
        return new CommandHandlerMetric(command.name, command.eventType());
    }

    public CommandHandlerMetric onNext() {
        onNextCount.incrementAndGet();
        return this;
    }

    public CommandHandlerMetric onCompleted() {
        calculateElapsed();
        hasCompleted.set(true);
        ReactoDashboardStream.publishCommandHandlerMetric(this);
        return this;
    }

    public CommandHandlerMetric onError(Throwable e) {
        calculateElapsed();
        hasError.set(Exceptions.getStackTrace(e));
        ReactoDashboardStream.publishCommandHandlerMetric(this);
        return this;
    }

    //properties
    public String commandName() {
        return commandName;
    }

    public String eventName() {
        return eventName;
    }

    public int eventCount() {
        return onNextCount.get();
    }

    public boolean hasCompleted() {
        return hasCompleted.get();
    }

    public boolean hasError() {
        return hasError.get() != null;
    }

    public Optional<String> error() {
        return Optional.ofNullable(hasError.get());
    }

    public long totalExecutionTimeInMs() {
        return timeElapsedInMs.get();
    }

    //private

    private void calculateElapsed() {
        timeElapsedInMs.set( Math.floorDiv( (System.nanoTime() - timeStarted), 100_000_0L));
    }

    @Override
    public String toString() {
        return "CommandHandlerMetric{" +
                "commandName='" + commandName + '\'' +
                ", eventName='" + eventName + '\'' +
                ", onNextCount=" + onNextCount +
                ", hasCompleted=" + hasCompleted +
                ", hasError=" + hasError +
                ", timeStarted=" + timeStarted +
                ", timeElapsedInMs=" + timeElapsedInMs +
                '}';
    }
}
