package net.soundvibe.reacto.mappers;

import com.google.protobuf.InvalidProtocolBufferException;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocketStream;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;
import net.soundvibe.reacto.client.commands.Services;
import net.soundvibe.reacto.client.errors.CannotDiscoverService;
import net.soundvibe.reacto.client.events.EventHandler;
import net.soundvibe.reacto.client.events.EventHandlers;
import net.soundvibe.reacto.internal.InternalEvent;
import net.soundvibe.reacto.internal.MessageMappers;
import net.soundvibe.reacto.internal.proto.Messages;
import net.soundvibe.reacto.internal.RuntimeProtocolBufferException;
import net.soundvibe.reacto.types.Command;
import net.soundvibe.reacto.types.Event;
import net.soundvibe.reacto.client.commands.Nodes;
import rx.Observable;

import java.io.*;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.soundvibe.reacto.utils.WebUtils.*;

/**
 * @author Linas on 2015.10.25.
 */
public interface Mappers {

    static byte[] internalEventToBytes(InternalEvent internalEvent) {
        return MessageMappers.toProtoBufEvent(internalEvent).toByteArray();
    }

    static byte[] commandToBytes(Command command) {
        return MessageMappers.toProtoBufCommand(command).toByteArray();
    }

    static InternalEvent fromBytesToInternalEvent(byte[] bytes) {
        try {
            return MessageMappers.toInternalEvent(Messages.Event.parseFrom(bytes));
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeProtocolBufferException("Cannot deserialize event from bytes: " + new String(bytes), e);
        }
    }

    static Event fromInternalEvent(InternalEvent internalEvent) {
        return Event.create(internalEvent.name, internalEvent.metaData, internalEvent.payload);
    }

    static Command fromBytesToCommand(byte[] bytes) {
        try {
            return MessageMappers.toCommand(Messages.Command.parseFrom(bytes));
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeProtocolBufferException("Cannot deserialize command from bytes: " + new String(bytes), e);
        }
    }

    static Optional<byte[]> exceptionToBytes(Throwable throwable) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(byteArrayOutputStream)) {
            oos.writeObject(throwable);
            return Optional.of(byteArrayOutputStream.toByteArray());
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    static Optional<Throwable> fromBytesToException(byte[] bytes) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return Optional.ofNullable(objectInputStream.readObject())
                    .map(o -> (Throwable) o);
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    static Supplier<Optional<EventHandlers>> mapToEventHandlers(Nodes nodes,
                                                                Function<URI, EventHandler> eventHandlerFactory) {
        return () -> Optional.ofNullable(nodes.mainURI())
                .map(eventHandlerFactory)
                .map(mainEventHandler -> new EventHandlers(mainEventHandler, Optional.empty()))
                .map(eventHandlers -> nodes.fallbackURI()
                        .map(eventHandlerFactory)
                        .map(eventHandlers::copy)
                        .orElse(eventHandlers));
    }

    static Observable<WebSocketStream> findService(String serviceName, ServiceDiscovery serviceDiscovery) {
        return Observable.<HttpClient>create(subscriber -> {
            subscriber.onStart();
            HttpEndpoint.getClient(serviceDiscovery, new JsonObject().put("name", serviceName),
                    asyncClient -> {
                        if (asyncClient.succeeded()) {
                            final HttpClient httpClient = asyncClient.result();
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onNext(httpClient);
                                subscriber.onCompleted();
                            }
                        }
                        if (asyncClient.failed()) {
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onError(new CannotDiscoverService("Unable to find service: " + serviceName, asyncClient.cause()));
                            }
                        }
                    }
            );
        }).map(httpClient -> httpClient.websocketStream(includeStartDelimiter(includeEndDelimiter(serviceName))));
    }
}
