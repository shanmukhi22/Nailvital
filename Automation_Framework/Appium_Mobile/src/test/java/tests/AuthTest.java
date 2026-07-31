package tests;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

public class AuthTest {

    private AppiumDriver driver;
    private WebDriverWait wait;

    @BeforeMethod
    public void setUp() throws MalformedURLException {
        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setCapability("platformName", "Android");
        caps.setCapability("appium:deviceName", "emulator-5554");
        caps.setCapability("appium:automationName", "UiAutomator2");
        caps.setCapability("appium:appPackage", "com.nailvital.app");
        caps.setCapability("appium:appActivity", ".MainActivity");

        driver = new AndroidDriver(new URL("http://127.0.0.1:4723/"), caps);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Test
    public void testValidLogin() {
        // Wait for the email field (Compose textfields often expose placeholder or label as text)
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(@text, 'you@email.com') or contains(@content-desc, 'you@email.com')]"))).sendKeys("test@example.com");
        
        // Enter password
        driver.findElement(By.xpath("//*[contains(@text, '••••••••') or contains(@content-desc, '••••••••')]")).sendKeys("SecurePassword123!");
        
        // Click Login
        driver.findElement(By.xpath("//*[@text='SIGN IN' or @content-desc='SIGN IN']")).click();
        
        // Assert we transition to the dashboard (look for Dashboard element text)
        boolean isDashboardVisible = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(@text, 'Health signals') or contains(@content-desc, 'Health signals')]"))).isDisplayed();
        Assert.assertTrue(isDashboardVisible, "Dashboard should be visible after login.");
    }

    @Test
    public void testInvalidLogin() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(@text, 'you@email.com') or contains(@content-desc, 'you@email.com')]"))).sendKeys("wrong@example.com");
        driver.findElement(By.xpath("//*[contains(@text, '••••••••') or contains(@content-desc, '••••••••')]")).sendKeys("badpass");
        driver.findElement(By.xpath("//*[@text='SIGN IN' or @content-desc='SIGN IN']")).click();
        
        // Assert error message
        String errorMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[contains(@text, 'Login failed') or contains(@content-desc, 'Login failed')]"))).getText();
        Assert.assertTrue(errorMsg.contains("Login failed"), "Error message should contain 'Login failed'");
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
