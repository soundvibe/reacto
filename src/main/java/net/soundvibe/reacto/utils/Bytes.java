package net.soundvibe.reacto.utils;

import java.util.*;

/**
 * @author Cipolinas on 2016.02.18.
 */
public interface Bytes {

    static boolean payloadsAreEqual(Optional<byte[]> left, Optional<byte[]> right) {
        if (left.isPresent() && right.isPresent()) {
            return Arrays.equals(left.get(), right.get());
        } else {
            return Objects.equals(left, right);
        }
    }
}
