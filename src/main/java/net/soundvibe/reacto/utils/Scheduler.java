package net.soundvibe.reacto.utils;

import io.vertx.core.logging.*;

import java.util.*;

/**
 * @author linas on 17.1.9.
 */
public interface Scheduler {

    Logger log = LoggerFactory.getLogger(Scheduler.class);

    static void scheduleAtFixedInterval(long intervalInMs, Runnable runnable, String nameOfTheTask) {
        new Timer(nameOfTheTask, true).scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Throwable e) {
                    log.error("Error while doing scheduled task: " + e);
                }
            }
        }, intervalInMs, intervalInMs);
    }

}
