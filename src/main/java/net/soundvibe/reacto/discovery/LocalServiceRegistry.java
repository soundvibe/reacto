package net.soundvibe.reacto.discovery;

import io.reactivex.Flowable;
import net.soundvibe.reacto.client.events.*;
import net.soundvibe.reacto.discovery.types.*;
import net.soundvibe.reacto.internal.ObjectId;
import net.soundvibe.reacto.mappers.ServiceRegistryMapper;
import net.soundvibe.reacto.server.CommandRegistry;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.types.json.JsonObject;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singletonList;

/**
 * @author OZY on 2017.01.25.
 */
public final class LocalServiceRegistry extends AbstractServiceRegistry {

    private final CommandRegistry commandRegistry;

    private final AtomicBoolean isClosed = new AtomicBoolean(true);

    public LocalServiceRegistry(ServiceRegistryMapper mapper,
                                CommandRegistry commandRegistry) {
        super(CommandHandlerRegistry.Builder.create()
                .register(
                        ServiceType.LOCAL,
                        serviceRecord -> new LocalCommandHandler(serviceRecord, commandRegistry))
                .build(),
                mapper);
        this.commandRegistry = commandRegistry;
    }

    @Override
    public Flowable<Any> register() {
        return Flowable.just(isClosed)
                .filter(AtomicBoolean::get)
                .doOnNext(closed -> closed.set(false))
                .map(__ -> Any.VOID);
    }

    @Override
    public Flowable<Any> unregister() {
        return Flowable.just(isClosed)
                .filter(closed -> !closed.get())
                .doOnNext(closed -> closed.set(true))
                .map(__ -> Any.VOID);
    }

    @Override
    protected Flowable<List<ServiceRecord>> findRecordsOf(Command command) {
        return commandRegistry.findCommand(CommandDescriptor.fromCommand(command)).isPresent() ?
                Flowable.just(singletonList(createRecord())) :
                Flowable.empty();
    }

    private ServiceRecord createRecord() {
        return ServiceRecord.create("local-service", Status.UP, ServiceType.LOCAL,
                ObjectId.get().toString(), JsonObject.empty(), JsonObject.empty());
    }
}
