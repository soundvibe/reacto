package net.soundvibe.reacto.utils;

import net.soundvibe.reacto.mappers.CommandRegistryMapper;
import net.soundvibe.reacto.types.*;

/**
 * @author OZY on 2017.01.10.
 */
public class DemoCommandRegistryMapper implements CommandRegistryMapper {

    @Override
    public <C> C toGenericCommand(Command command, Class<? extends C> commandClass) {
        return null;
    }

    @Override
    public <E> Event toEvent(E genericEvent) {
        return null;
    }
}
