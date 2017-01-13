package net.soundvibe.reacto.metric;

import net.soundvibe.reacto.types.Command;

import java.util.concurrent.atomic.*;

/**
 * @author OZY on 2017.01.12.
 */
public final class CommandProcessorMetric {

    private final String commandName;
    private final String eventName;
    private final AtomicInteger onNextCount;
    final AtomicInteger commandCount;
    private final AtomicInteger hasCompleted;
    private final AtomicInteger hasErrors;
    private final long timeStarted;
    final AtomicLong timeElapsedInMs;

    private CommandProcessorMetric(String commandName, String eventName) {
        this.commandName = commandName;
        this.eventName = eventName;
        this.onNextCount = new AtomicInteger(0);
        this.commandCount = new AtomicInteger(1);
        this.hasCompleted = new AtomicInteger(0);
        this.hasErrors = new AtomicInteger(0);
        this.timeElapsedInMs = new AtomicLong(0L);
        this.timeStarted = System.nanoTime();
    }

    private CommandProcessorMetric(String commandName, String eventName, int eventCount,
                                   int commandCount, int hasCompleted, int hasErrors, long timeElapsedInMs) {
        this.commandName = commandName;
        this.eventName = eventName;
        this.onNextCount = new AtomicInteger(eventCount);
        this.commandCount = new AtomicInteger(commandCount);
        this.hasCompleted = new AtomicInteger(hasCompleted);
        this.hasErrors = new AtomicInteger(hasErrors);
        this.timeElapsedInMs = new AtomicLong(timeElapsedInMs);
        this.timeStarted = System.nanoTime();
    }

    public static CommandProcessorMetric of(Command command) {
        return new CommandProcessorMetric(command.name, command.eventType());
    }

    public static CommandProcessorMetric of(String commandName, String eventName) {
        return new CommandProcessorMetric(commandName, eventName);
    }

    public CommandProcessorMetric accumulate(CommandProcessorMetric with) {
        return new CommandProcessorMetric(
                commandName,
                eventName,
                onNextCount.addAndGet(with.onNextCount.get()),
                commandCount.addAndGet(with.commandCount.get()),
                hasCompleted.addAndGet(with.hasCompleted.get()),
                hasErrors.addAndGet(with.hasErrors.get()),
                timeElapsedInMs.addAndGet(with.timeElapsedInMs.get())
        );
    }

    public CommandProcessorMetric onNext() {
        onNextCount.incrementAndGet();
        return this;
    }

    public CommandProcessorMetric onCompleted() {
        calculateElapsed();
        hasCompleted.incrementAndGet();
        ReactoDashboardStream.publishCommandHandlerMetric(this);
        return this;
    }

    public CommandProcessorMetric onError(Throwable e) {
        calculateElapsed();
        hasErrors.incrementAndGet();
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

    public int commandCount() {
        return commandCount.get();
    }

    public int completed() {
        return hasCompleted.get();
    }

    public int errors() {
        return hasErrors.get();
    }


    public long totalExecutionTimeInMs() {
        return timeElapsedInMs.get();
    }

    public long avgExecutionTimeInMs() {
        final int count = commandCount();
        return count < 1 ? 0L : Math.floorDiv(totalExecutionTimeInMs(), count);
    }

    public int commandsPerSecond(long delayInMs) {
        return delayInMs < 1L ? 0 : (int) (((double) commandCount() / delayInMs) * 1000);
    }

    //private

    private void calculateElapsed() {
        timeElapsedInMs.set( Math.floorDiv( (System.nanoTime() - timeStarted), 100_000_0L));
    }

    @Override
    public String toString() {
        return "CommandProcessorMetric{" +
                "commandName='" + commandName + '\'' +
                ", eventName='" + eventName + '\'' +
                ", onNextCount=" + onNextCount +
                ", completed=" + hasCompleted +
                ", errors=" + hasErrors +
                ", timeStarted=" + timeStarted +
                ", timeElapsedInMs=" + timeElapsedInMs +
                ", avgExecutionTimeInMs=" + avgExecutionTimeInMs() +
                '}';
    }
}
