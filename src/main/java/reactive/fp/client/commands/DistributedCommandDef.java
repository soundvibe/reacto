package reactive.fp.client.commands;

import reactive.fp.utils.WebUtils;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

/**
 * @author OZY on 2015.11.13.
 */
public class DistributedCommandDef {

    public final String name;
    public final CommandNodes nodes;

    public DistributedCommandDef(String name, CommandNodes nodes) {
        this.name = name;
        this.nodes = nodes;
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
        DistributedCommandDef that = (DistributedCommandDef) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "DistributedCommandDef{" +
                "name='" + name + '\'' +
                ", nodes=" + nodes +
                '}';
    }
}
