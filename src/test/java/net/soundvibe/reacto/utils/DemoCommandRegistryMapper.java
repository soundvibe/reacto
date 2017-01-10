package net.soundvibe.reacto.utils;

import net.soundvibe.reacto.mappers.CommandRegistryMapper;
import net.soundvibe.reacto.types.*;

/**
 * @author OZY on 2017.01.10.
 */
public class DemoCommandRegistryMapper implements CommandRegistryMapper {

    @Override
    public <C> C toGenericCommand(Command command, Class<? extends C> commandClass) {
        if (!commandClass.equals(MakeDemo.class)) {
            throw new IllegalArgumentException("Expected MakeDemo command class but got: " + commandClass);
        }
        return commandClass.cast(new MakeDemo(command.get("name")));
    }

    @Override
    public <E> TypedEvent toEvent(E genericEvent) {
        if (!genericEvent.getClass().equals(DemoMade.class)) {
            throw new IllegalArgumentException("Expected DemoMade event class but got: " + genericEvent.getClass());
        }
        final DemoMade demoMade = (DemoMade) genericEvent;
        return TypedEvent.create(demoMade.getClass(), MetaData.of("name", demoMade.name));
    }
}
