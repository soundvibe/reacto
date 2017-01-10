package net.soundvibe.reacto.mappers;

import net.soundvibe.reacto.types.*;

/**
 * @author OZY on 2017.01.10.
 */
public interface ServiceRegistryMapper {

    <C> TypedCommand toCommand(C genericCommand);

    <E> E toGenericEvent(TypedEvent event, Class<? extends E> eventClass);

}
