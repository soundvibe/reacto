package reactive.fp.client.commands;

import reactive.fp.utils.WebUtils;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * @author OZY on 2015.11.13.
 */
public class CommandDef {

    public final String name;
    private final CommandNodes nodes;

    private CommandDef(String name, CommandNodes nodes) {
        Objects.requireNonNull(name, "Command name cannot be null");
        this.name = name;
        this.nodes = nodes;
    }

    public static CommandDef ofMain(String commandName, String mainNode) {
        return new CommandDef(commandName, new CommandNodes(mainNode, Optional.empty()));
    }

    public static CommandDef ofMainAndFallback(String commandName, String mainNode, String fallbackNode) {
        return new CommandDef(commandName, new CommandNodes(mainNode, Optional.of(fallbackNode)));
    }

    public URI mainURI() {
        return WebUtils.resolveWsURI(WebUtils.includeEndDelimiter(nodes.mainNode) + name);
    }

    public Optional<URI> fallbackURI() {
        return nodes.fallbackNode
                .map(node -> WebUtils.includeEndDelimiter(node) + name)
                .map(WebUtils::resolveWsURI);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommandDef that = (CommandDef) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(nodes, that.nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, nodes);
    }

    @Override
    public String toString() {
        return "CommandDef{" +
                "name='" + name + '\'' +
                ", nodes=" + nodes +
                '}';
    }
}
