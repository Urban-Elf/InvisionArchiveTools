package com.urbanelf.iat.ic.workers;

import com.urbanelf.iat.WebDriverFactory;
import com.urbanelf.iat.ic.IC;
import com.urbanelf.iat.ic.workers.state.ICWorkerState;
import com.urbanelf.iat.ic.workers.state.ICWorkerStateObserver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

public abstract class ICWorker<T> implements Runnable {
    private final String title;
    private IC ic;
    private ICWorkerState currentState;
    private final ArrayList<ICWorkerStateObserver> stateObservers;
    private Object clientObject;
    private WebDriver driver;
    private final Consumer<T> resultConsumer;


    public ICWorker(String title, Consumer<T> onCompletion) {
        this.title = title;
        this.resultConsumer = onCompletion;
        stateObservers = new ArrayList<>();
    }

    @Override
    public void run() {
        // Initialize
        setState(ICWorkerState.INITIALIZATION);
        // Setup driver
        driver = WebDriverFactory.createChromeDriver();
        // Load IC
        driver.get(ic.getRootUrl());

        // Authenticate
        authenticate();
        setState(ICWorkerState.AUTH_REQUIRED);
    }

    public abstract void authenticate();

    protected void await(ExpectedCondition<?>... expectedConditions) {
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(driver -> Arrays.stream(expectedConditions)
                        .map(ec -> ec.apply(driver))
                        .allMatch(value -> value != null && (Boolean.class != value.getClass() || Boolean.TRUE.equals(value))));
    }

    public String getTitle() {
        return title;
    }

    public IC getIC() {
        if (ic == null)
            throw new ICWorkerException("IC not set; use with " + ICWorkerStack.class.getSimpleName());
        return ic;
    }

    protected void setIC(IC ic) {
        this.ic = ic;
    }

    public WebDriver getDriver() {
        if (driver == null)
            throw new ICWorkerException("Driver not initialized");
        return driver;
    }

    protected Consumer<T> getResultConsumer() {
        return resultConsumer;
    }

    public ICWorkerState getCurrentState() {
        return currentState;
    }

    protected Object setState(ICWorkerState state) {
        this.currentState = state;
        getStateObservers().forEach(observer -> observer.stateChanged(state));
        clientObject = null; // Reset client object
        if (!state.isProgressive()) {
            while (clientObject == null) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return clientObject;
        }
        return null;
    }

    public void pushClientObject(Object clientObject) {
        this.clientObject = clientObject;
    }

    protected ArrayList<ICWorkerStateObserver> getStateObservers() {
        return stateObservers;
    }

    public void addStateObserver(ICWorkerStateObserver stateObserver) {
        stateObservers.add(stateObserver);
    }

    public void removeStateObserver(ICWorkerStateObserver stateObserver) {
        stateObservers.remove(stateObserver);
    }

    public void clearStateObservers() {
        stateObservers.clear();
    }
}
