package com.urbanelf.ima.ic.workers.state;

public interface ICWorkerStateObserver {
    void stateChanged(ICWorkerState state);

    default void progressChanged(float progress) {
    };
}
