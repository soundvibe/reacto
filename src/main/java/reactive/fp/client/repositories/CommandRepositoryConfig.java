package reactive.fp.client.repositories;

import reactive.fp.client.commands.CommandDef;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author OZY on 2015.11.13.
 */
public class CommandRepositoryConfig {

    private final Map<String, CommandDef> commands;

    private CommandRepositoryConfig(Map<String, CommandDef> commands) {
        this.commands = commands;
    }

    public static CommandRepositoryConfig from(CommandDef... entries) {
        Objects.requireNonNull(entries, "At least one entry should be provided");
        Map<String, CommandDef> commands = new ConcurrentHashMap<>(entries.length);
        for (CommandDef entry: entries) {
            commands.put(entry.name, entry);
        }
        return new CommandRepositoryConfig(commands);
    }

    public Optional<CommandDef> findDistributedCommand(String key) {
        return Optional.ofNullable(commands.get(key));
    }

    @Override
    public String toString() {
        return "CommandRepositoryConfig{" +
                "commands=" + commands +
                '}';
    }
}
