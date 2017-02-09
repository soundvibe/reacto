package net.soundvibe.reacto.utils;

import java.net.*;

/**
 * @author OZY on 2015.11.13.
 */
public interface WebUtils {

    static String includeEndDelimiter(String url) {
        if (url == null) return null;
        return url.endsWith("/") ? url : url + "/";
    }

    static String excludeEndDelimiter(String url) {
        if (url == null) return null;
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    static String excludeStartDelimiter(String url) {
        if (url == null) return null;
        return url.startsWith("/") ? url.substring(1, url.length()) : url;
    }

    static String includeStartDelimiter(String url) {
        if (url == null) return null;
        return url.startsWith("/") ? url : "/" + url;
    }

    static URI resolveWsURI(String url) {
        return url.startsWith("http") ? URI.create(url.replaceFirst("http", "ws")):
                url.startsWith("https") ? URI.create(url.replaceFirst("https", "wss")) :
                        URI.create(url);
    }


    static String getLocalAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            try {
                return InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e1) {
                return "localhost";
            }
        }
    }
}
