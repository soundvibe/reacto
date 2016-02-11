package io.reacto.utils;

import java.io.*;

/**
 * @author OZY on 2016.02.09.
 */
public interface Exceptions {

    static String getStackTrace(Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
