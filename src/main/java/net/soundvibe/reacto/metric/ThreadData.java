package net.soundvibe.reacto.metric;

/**
 * @author OZY on 2017.01.13.
 */
public final class ThreadData {

    public final int threadCount;
    public final int daemonThreadCount;
    public final int peakThreadCount;

    public ThreadData(int threadCount, int daemonThreadCount, int peakThreadCount) {
        this.threadCount = threadCount;
        this.daemonThreadCount = daemonThreadCount;
        this.peakThreadCount = peakThreadCount;
    }
}
