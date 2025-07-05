package com.urbanelf.ima.ic;

public final class IC4 extends IC {
    private final transient String auth;

    public IC4(String rootUrl) {
        super(rootUrl);
        this.auth = getRootUrl() + "login/";
    }

    @Override
    public String messenger(int messengerId) {
        return getRootUrl() + messengerId + "/";
    }

    @Override
    public int getVersionNumber() {
        return 4;
    }

    @Override
    public String auth() {
        return auth;
    }

    @Override
    public String toString() {
        return getRootUrl() + " (v4)";
    }
}
