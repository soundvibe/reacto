package net.soundvibe.reacto.client.events;

import io.reactivex.Flowable;
import net.soundvibe.reacto.discovery.types.ServiceRecord;
import net.soundvibe.reacto.errors.CannotDiscoverService;
import net.soundvibe.reacto.server.CommandRegistry;
import net.soundvibe.reacto.types.*;

import java.util.Objects;

/**
 * @author OZY on 2017.01.25.
 */
public final class LocalCommandHandler implements CommandHandler {

    private final ServiceRecord serviceRecord;
    private final CommandRegistry commandRegistry;

    public LocalCommandHandler(ServiceRecord serviceRecord, CommandRegistry commandRegistry) {
        Objects.requireNonNull(serviceRecord, "serviceRecord cannot be null");
        Objects.requireNonNull(commandRegistry, "commandRegistry cannot be null");
        this.serviceRecord = serviceRecord;
        this.commandRegistry = commandRegistry;
    }

    @Override
    public Flowable<Event> observe(Command command) {
        return commandRegistry.findCommand(CommandDescriptor.fromCommand(command))
                .map(commandExecutor -> commandExecutor.execute(command))
                .orElseGet(() -> Flowable.error(new CannotDiscoverService("Unable to find service for " + command)));
    }

    @Override
    public ServiceRecord serviceRecord() {
        return this.serviceRecord;
    }
}
