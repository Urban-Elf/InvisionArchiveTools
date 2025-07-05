package com.urbanelf.iat.ic;

public final class IC5 extends IC {
    private final transient String auth;

    public IC5(String rootUrl) {
        super(rootUrl);
        this.auth = getRootUrl() + "login/";
    }

    @Override
    public String messenger(int messengerId) {
        return getRootUrl() + messengerId + "/";
    }

    @Override
    public int getVersionNumber() {
        return 5;
    }

    @Override
    public String auth() {
        return auth;
    }

    @Override
    public String toString() {
        return getRootUrl() + " (v5)";
    }
}
