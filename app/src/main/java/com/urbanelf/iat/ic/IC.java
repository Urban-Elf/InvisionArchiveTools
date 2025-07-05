package com.urbanelf.iat.ic;

public abstract class IC {
    private final String rootUrl;

    public static String PHP_data(String urlString, String data) {
        return urlString + (urlString.endsWith("/") ? "" : "/") + (data.startsWith("?") ? "" : "?") + data;
    }

    public IC(String rootUrl) {
        this.rootUrl = rootUrl + "/";
    }

    public abstract String auth();

    public abstract String messenger(int messengerId);

    public abstract int getVersionNumber();

    public String getRootUrl() {
        return rootUrl;
    }
}
