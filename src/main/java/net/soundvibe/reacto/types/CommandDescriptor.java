package net.soundvibe.reacto.types;

import java.util.Objects;

/**
 * @author OZY on 2017.01.10.
 */
public final class CommandDescriptor {

    public final static String COMMAND = "commandType";
    public final static String EVENT = "eventType";

    public final String commandType;
    public final String eventType;

    private CommandDescriptor(String commandType, String eventType) {
        this.commandType = commandType;
        this.eventType = eventType;
    }

    public static CommandDescriptor of(String commandName) {
        return new CommandDescriptor(commandName, "");
    }

    public static CommandDescriptor ofNames(String commandType, String eventType) {
        return new CommandDescriptor(commandType, eventType);
    }

    public static CommandDescriptor ofTypes(Class<?> commandType, Class<?> eventType) {
        return new CommandDescriptor(commandType.getName(), eventType.getName());
    }

    public static CommandDescriptor fromCommand(Command receivedCommand) {
        return new CommandDescriptor(receivedCommand.name, receivedCommand.eventType());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CommandDescriptor that = (CommandDescriptor) o;
        return Objects.equals(commandType, that.commandType) &&
                Objects.equals(eventType, that.eventType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandType, eventType);
    }

    @Override
    public String toString() {
        return "CommandDescriptor{" +
                "commandType='" + commandType + '\'' +
                ", eventType='" + eventType + '\'' +
                '}';
    }


}
