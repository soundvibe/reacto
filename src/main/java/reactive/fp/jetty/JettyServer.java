package reactive.fp.jetty;

import reactive.fp.commands.CommandRegistry;
import reactive.fp.config.WebServerConfig;
import reactive.fp.types.WebServer;
import spark.Spark;

import static reactive.fp.utils.WebUtils.includeEndDelimiter;
import static reactive.fp.utils.WebUtils.includeStartDelimiter;
import static spark.Spark.*;

/**
 * @author Cipolinas on 2015.11.16.
 */
public class JettyServer implements WebServer {

    private final WebServerConfig config;
    private final CommandRegistry commands;

    public JettyServer(WebServerConfig config, CommandRegistry commands) {
        this.config = config;
        this.commands = commands;
    }

    @Override
    public void start() {
        port(config.port);
        setupRoutes();
        init();
        awaitInitialization();
    }

    @Override
    public void stop() {
        Spark.stop();
    }

    @Override
    public void setupRoutes() {
        commands.foreach(key -> webSocket(root() + key, JettyCommandHandler.class));
    }

    protected String root() {
        return includeEndDelimiter(includeStartDelimiter(config.root));
    }
}
