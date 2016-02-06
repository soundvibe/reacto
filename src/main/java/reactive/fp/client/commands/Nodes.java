package reactive.fp.client.commands;

import reactive.fp.utils.WebUtils;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * @author OZY on 2015.11.13.
 */
public final class Nodes {

    private final CommandNodes nodes;

    private Nodes(CommandNodes nodes) {
        this.nodes = nodes;
    }

    public static Nodes ofMain(String mainNode) {
        return new Nodes(new CommandNodes(mainNode, Optional.empty()));
    }

    public static Nodes ofMainAndFallback(String mainNode, String fallbackNode) {
        return new Nodes(new CommandNodes(mainNode, Optional.of(fallbackNode)));
    }

    public URI mainURI(String commandName) {
        return WebUtils.resolveWsURI(WebUtils.includeEndDelimiter(nodes.mainNode) + commandName);
    }

    public Optional<URI> fallbackURI(String commandName) {
        return nodes.fallbackNode
                .map(node -> WebUtils.includeEndDelimiter(node) + commandName)
                .map(WebUtils::resolveWsURI);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Nodes that = (Nodes) o;
        return Objects.equals(nodes, that.nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodes);
    }

    @Override
    public String toString() {
        return "CommandDef{" +
                "nodes=" + nodes +
                '}';
    }
}
