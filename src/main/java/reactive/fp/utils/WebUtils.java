package reactive.fp.utils;

import java.net.URI;

/**
 * @author OZY on 2015.11.13.
 */
public interface WebUtils {

    static String includeEndDelimiter(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    static String includeStartDelimiter(String address) {
        return address.startsWith("/") ? address : "/" + address;
    }

    static URI resolveWsURI(String url) {
        return url.startsWith("http") ? URI.create(url.replaceFirst("http", "ws")):
                url.startsWith("https") ? URI.create(url.replaceFirst("https", "wss")) :
                        URI.create(url);
    }
}
