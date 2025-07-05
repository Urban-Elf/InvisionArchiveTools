package com.urbanelf.ima.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLUtils {
    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("^(https?://)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}/?$", Pattern.CASE_INSENSITIVE);

    public static boolean isURLHTTP(String url) {
        return PROTOCOL_PATTERN.matcher(url).find();
        /*try {
            final URI uri = new URI(url);
            return uri.isAbsolute() && uri.getScheme() == null || (uri.getScheme().equals("http") || uri.getScheme().equals("https"));
        } catch (URISyntaxException e) {
            return false;
        }*/
    }

    public static String normalizeURLString(String url) {
        url = url.toLowerCase();
        if (url.startsWith("http://"))
            url = url.replaceFirst("http", "https");
        if (!url.startsWith("https://")) {
            url = "https://" + url;
        }
        if (url.endsWith("/"))
            url = url.substring(0, url.length()-1);
        return url;
    }

}
