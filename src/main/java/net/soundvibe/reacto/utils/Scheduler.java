package net.soundvibe.reacto.utils;

import java.util.*;

/**
 * @author linas on 17.1.9.
 */
public interface Scheduler {

    static Timer scheduleAtFixedInterval(long intervalInMs, Runnable runnable, String nameOfTheTask) {
        final Timer timer = new Timer(nameOfTheTask, true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Throwable e) {
                    //do-nothing
                }
            }
        }, intervalInMs, intervalInMs);
        return timer;
    }

}
