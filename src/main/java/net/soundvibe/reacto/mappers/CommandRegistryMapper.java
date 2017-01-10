package net.soundvibe.reacto.mappers;

import net.soundvibe.reacto.types.*;

/**
 * @author OZY on 2017.01.10.
 */
public interface CommandRegistryMapper {

    <C> C toGenericCommand(TypedCommand command, Class<? extends C> commandClass);

    <E> TypedEvent toEvent(E genericEvent);

}
