package reactive.fp.config;

import reactive.fp.types.DistributedCommandDef;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author OZY on 2015.11.13.
 */
public class CommandRepositoryConfig {

    private final Map<String, DistributedCommandDef> commands;

    public CommandRepositoryConfig(Map<String, DistributedCommandDef> commands) {
        this.commands = commands;
    }

    public static CommandRepositoryConfig create(DistributedCommandDef... entries) {
        Map<String, DistributedCommandDef> commands = new ConcurrentHashMap<>(entries.length);
        for (DistributedCommandDef entry: entries) {
            commands.put(entry.name, entry);
        }
        return new CommandRepositoryConfig(commands);
    }

    public Optional<DistributedCommandDef> findDistributedCommand(String key) {
        return Optional.ofNullable(commands.get(key));
    }

    @Override
    public String toString() {
        return "CommandRepositoryConfig{" +
                "commands=" + commands +
                '}';
    }
}
