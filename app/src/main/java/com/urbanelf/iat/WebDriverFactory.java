package com.urbanelf.iat;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.util.Collections;

public final class WebDriverFactory {

    private WebDriverFactory() {
    }

    public static ChromeDriver createChromeDriver() {
        final ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");

        // Fixing 255 Error crashes
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        // Options to trick bot detection
        // Removing webdriver property
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", null);

        // Changing the user agent / browser fingerprint
        //options.addArguments("window-size=1920,1080");
        //options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36");

        // Other
        options.addArguments("disable-infobars");

        final ChromeDriver driver = new ChromeDriver(options);
        driver.executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

        return driver;
    }
}
