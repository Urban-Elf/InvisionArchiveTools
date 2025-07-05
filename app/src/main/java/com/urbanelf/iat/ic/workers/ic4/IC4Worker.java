package com.urbanelf.iat.ic.workers.ic4;

import com.urbanelf.iat.ic.IC;
import com.urbanelf.iat.ic.workers.ICWorker;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.function.Consumer;

public abstract class IC4Worker<T> extends ICWorker<T> {
    public IC4Worker(String name, Consumer<T> onCompletion) {
        super(name, onCompletion);
    }

    @Override
    public void authenticate() {
        final IC ic = getIC();
        final WebDriver driver = getDriver();

        driver.get(ic.auth());

        /*await(ExpectedConditions.presenceOfElementLocated(By.id("auth")),
                ExpectedConditions.presenceOfElementLocated(By.name("password")),
                ExpectedConditions.presenceOfElementLocated(By.name("_processLogin")));*/

        /*final WebElement authField = driver.findElement(By.id("auth"));
        final WebElement passwordField = driver.findElement(By.name("password"));

        authField.sendKeys(auth);
        passwordField.sendKeys(password);

        final WebElement loginButton = driver.findElement(By.name("_processLogin"));
        await(_driver -> loginButton.isDisplayed());

        loginButton.click();*/

        // FIXME: await button prompt...

        //await(ExpectedConditions.urlToBe(IC.PHP_data(ic.getRootUrl(), "_fromLogin=1")));

    }

    // FIXME:  Is having ICWorkerState as U generic for ICWorker NOT suitable for our usecase?
    //  Like, when we use StateObservers for UI updating, will we be in a dillema trying to provide
    //  abstraction with WorkerStates for the type of ICWorker child we're executing?


}
