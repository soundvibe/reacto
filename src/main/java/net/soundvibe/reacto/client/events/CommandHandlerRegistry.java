package net.soundvibe.reacto.client.events;

import net.soundvibe.reacto.discovery.types.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * @author Linas on 2017.01.18.
 */
public final class CommandHandlerRegistry {

    private final Map<ServiceType, CommandHandlerFactory> handlers;

    private final Map<ServiceRecord, CommandHandler> cache;

    private static final CommandHandlerRegistry EMPTY = new CommandHandlerRegistry(Collections.emptyMap());

    public static CommandHandlerRegistry empty() {
        return EMPTY;
    }

    private CommandHandlerRegistry(Map<ServiceType, CommandHandlerFactory> handlers) {
        this.handlers = handlers;
        this.cache = new ConcurrentHashMap<>(handlers.size());
    }

    public Optional<CommandHandlerFactory> findFactory(ServiceType serviceType) {
        return Optional.ofNullable(handlers.get(serviceType));
    }

    public Stream<CommandHandler> find(ServiceRecord serviceRecord) {
        return findFactory(serviceRecord.type)
                .map(factory -> Stream.of(cache.computeIfAbsent(serviceRecord, factory::create)))
                .orElseGet(Stream::empty);
    }

    public static final class Builder {

        private final Map<ServiceType, CommandHandlerFactory> handlers = new EnumMap<>(ServiceType.class);

        public static Builder create() {
            return new Builder();
        }

        public Builder register(ServiceType serviceType, CommandHandlerFactory commandHandlerFactory) {
            handlers.put(serviceType, commandHandlerFactory);
            return this;
        }

        public CommandHandlerRegistry build() {
            return new CommandHandlerRegistry(this.handlers);
        }


    }


}
