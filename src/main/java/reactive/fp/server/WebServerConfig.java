package reactive.fp.server;

/**
 * @author Linas on 2015.11.12.
 */
public final class WebServerConfig {

    public final int port;
    public final String root;

    public WebServerConfig(int port, String root) {
        this.port = port;
        this.root = root;
    }

    @Override
    public String toString() {
        return "WebServerConfig{" +
                "port=" + port +
                ", root='" + root + '\'' +
                '}';
    }
}
