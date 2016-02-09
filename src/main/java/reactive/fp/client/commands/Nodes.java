package reactive.fp.client.commands;

import reactive.fp.utils.WebUtils;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * @author OZY on 2015.11.13.
 */
public final class Nodes {

    private final String mainNode;
    private final Optional<String> fallbackNode;

    private Nodes(String mainNode, Optional<String> fallbackNode) {
        Objects.requireNonNull(mainNode, "Main node cannot be null");
        this.mainNode = mainNode;
        this.fallbackNode = fallbackNode;
    }

    public static Nodes ofMain(String mainNode) {
        return new Nodes(mainNode, Optional.empty());
    }

    public static Nodes ofMainAndFallback(String mainNode, String fallbackNode) {
        return new Nodes(mainNode, Optional.ofNullable(fallbackNode));
    }

    public URI mainURI(String commandName) {
        return WebUtils.resolveWsURI(WebUtils.includeEndDelimiter(mainNode) + commandName);
    }

    public Optional<URI> fallbackURI(String commandName) {
        return fallbackNode
                .map(node -> WebUtils.includeEndDelimiter(node) + commandName)
                .map(WebUtils::resolveWsURI);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Nodes nodes = (Nodes) o;
        return Objects.equals(mainNode, nodes.mainNode) &&
                Objects.equals(fallbackNode, nodes.fallbackNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mainNode, fallbackNode);
    }

    @Override
    public String toString() {
        return "Nodes{" +
                "mainNode='" + mainNode + '\'' +
                ", fallbackNode=" + fallbackNode +
                '}';
    }
}
