package reactive.fp.client.commands;

import reactive.fp.utils.WebUtils;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * @author OZY on 2015.11.13.
 */
public class CommandDef<U> {

    public final String name;
    public final Class<U> eventClass;
    private final CommandNodes nodes;

    private CommandDef(String name, Class<U> eventClass, CommandNodes nodes) {
        Objects.requireNonNull(name, "Command name cannot be null");
        Objects.requireNonNull(eventClass, "Event class cannot be null");
        this.name = name;
        this.eventClass = eventClass;
        this.nodes = nodes;
    }

    public static <U> CommandDef<U> ofMain(String commandName, String mainNode, Class<U> eventClass) {
        return new CommandDef<>(commandName, eventClass, new CommandNodes(mainNode, Optional.empty()));
    }

    public static <U> CommandDef<U> ofMainAndFallback(String commandName, String mainNode, String fallbackNode, Class<U> eventClass) {
        return new CommandDef<>(commandName, eventClass, new CommandNodes(mainNode, Optional.of(fallbackNode)));
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
                Objects.equals(eventClass, that.eventClass) &&
                Objects.equals(nodes, that.nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, eventClass, nodes);
    }

    @Override
    public String toString() {
        return "CommandDef{" +
                "name='" + name + '\'' +
                ", eventClass=" + eventClass +
                ", nodes=" + nodes +
                '}';
    }
}
