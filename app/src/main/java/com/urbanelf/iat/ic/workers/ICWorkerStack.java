package com.urbanelf.iat.ic.workers;

import com.urbanelf.iat.ic.IC;
import com.urbanelf.iat.ic.workers.state.ICWorkerState;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ICWorkerStack<T extends ICWorker<?>> {
    private final ArrayList<T> workers;
    private final ExecutorService executorService;
    private IC ic;

    public ICWorkerStack(int maximumConcurrentWorkers) {
        if (maximumConcurrentWorkers < 0)
            throw new IllegalArgumentException("maximumConcurrentWorkers must be > 0");
        workers = new ArrayList<>();
        executorService = Executors.newFixedThreadPool(maximumConcurrentWorkers);
    }

    public void pushWorker(T worker) {
        if (ic == null)
            throw new ICWorkerException("IC not set; use setIC(IC) before adding new workers");
        worker.setIC(ic);
        worker.addStateObserver(state -> {
            if (state == ICWorkerState.COMPLETED)
                workers.remove(worker);
        });
        workers.add(worker);
        executorService.execute(worker);
    }

    public IC getIC() {
        return ic;
    }

    public void setIC(IC ic) {
        this.ic = ic;
        workers.forEach(worker -> worker.setIC(ic));
    }

    ArrayList<T> getWorkers() {
        return workers;
    }
}
