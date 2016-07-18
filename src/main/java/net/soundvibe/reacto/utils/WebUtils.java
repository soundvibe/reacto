package net.soundvibe.reacto.utils;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * @author OZY on 2015.11.13.
 */
public interface WebUtils {

    static String includeEndDelimiter(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    static String excludeEndDelimiter(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    static String excludeStartDelimiter(String url) {
        return url.startsWith("/") ? url.substring(1, url.length()) : url;
    }

    static String includeStartDelimiter(String address) {
        return address.startsWith("/") ? address : "/" + address;
    }

    static URI resolveWsURI(String url) {
        return url.startsWith("http") ? URI.create(url.replaceFirst("http", "ws")):
                url.startsWith("https") ? URI.create(url.replaceFirst("https", "wss")) :
                        URI.create(url);
    }


    static String getLocalAddress() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e1) {
                return "localhost";
            }
        }
    }
}
