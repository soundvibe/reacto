package net.soundvibe.reacto.types;

import net.soundvibe.reacto.utils.Exceptions;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author OZY on 2016.02.05.
 */
public class ReactiveException extends RuntimeException {

    public final String className;
    public final String message;
    public final String stackTrace;

    public ReactiveException(String className, String message, String stackTrace) {
        super(message);
        this.className = className;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    public static ReactiveException from(Throwable throwable) {
        return new ReactiveException(throwable.getClass().getName(),
                throwable.getMessage() == null ? throwable.toString() : throwable.getMessage(),
                Exceptions.getStackTrace(throwable));
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        printStackTrace(s::println);
    }

    @Override
    public void printStackTrace(PrintStream s) {
        printStackTrace(s::println);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReactiveException that = (ReactiveException) o;
        return Objects.equals(className, that.className) &&
                Objects.equals(message, that.message) &&
                Objects.equals(stackTrace, that.stackTrace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, message, stackTrace);
    }

    private void printStackTrace(Consumer<String> printLnConsumer) {
        if (stackTrace == null) return;
        String lines[] = stackTrace.split("\\r?\\n");
        for (String line: lines) {
            printLnConsumer.accept(line);
        }
    }

    @Override
    public String toString() {
        return "ReactiveException{" +
                "className='" + className + '\'' +
                ", message='" + message + '\'' +
                ", stackTrace='" + stackTrace + '\'' +
                '}';
    }
}
