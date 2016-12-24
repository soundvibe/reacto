package net.soundvibe.reacto.types;

import net.soundvibe.reacto.utils.WebUtils;

import java.net.URI;
import java.util.*;
import java.util.stream.*;

/**
 * @author OZY on 2015.11.13.
 */
public final class Nodes {

    private final Collection<URI> nodes;

    private Nodes(String... nodesURIs) {
        Objects.requireNonNull(nodesURIs, "nodesURIs cannot be null");
        this.nodes = Stream.of(nodesURIs)
                .map(uri -> WebUtils.resolveWsURI(WebUtils.includeEndDelimiter(uri)))
                .collect(Collectors.toList());
    }

    private Nodes(Collection<String> nodesURIs) {
        Objects.requireNonNull(nodesURIs, "nodesURIs cannot be null");
        this.nodes = nodesURIs.stream()
                .map(uri -> WebUtils.resolveWsURI(WebUtils.includeEndDelimiter(uri)))
                .collect(Collectors.toList());
    }

    public static Nodes of(String... nodeURIs) {
        return new Nodes(nodeURIs);
    }

    public static Nodes of(Collection<String> nodeURIs) {
        return new Nodes(nodeURIs);
    }

    public Stream<URI> stream() {
        return nodes.stream();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Nodes nodes1 = (Nodes) o;
        return Objects.equals(nodes, nodes1.nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodes);
    }

    @Override
    public String toString() {
        return "Nodes{" +
                "nodes=" + nodes +
                '}';
    }
}
