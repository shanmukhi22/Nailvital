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
        // Using a dummy app package since the actual APK path isn't provided yet
        caps.setCapability("appium:appPackage", "com.nailvital.app");
        caps.setCapability("appium:appActivity", ".MainActivity");

        driver = new AndroidDriver(new URL("http://127.0.0.1:4723/"), caps);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Test
    public void testValidLogin() {
        // Wait for the email field
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("com.nailvital.app:id/email_input"))).sendKeys("test@example.com");
        
        // Enter password
        driver.findElement(By.id("com.nailvital.app:id/password_input")).sendKeys("SecurePassword123!");
        
        // Click Login
        driver.findElement(By.id("com.nailvital.app:id/login_button")).click();
        
        // Assert we transition to the dashboard (e.g., dashboard title becomes visible)
        boolean isDashboardVisible = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("com.nailvital.app:id/dashboard_title"))).isDisplayed();
        Assert.assertTrue(isDashboardVisible, "Dashboard should be visible after login.");
    }

    @Test
    public void testInvalidLogin() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("com.nailvital.app:id/email_input"))).sendKeys("wrong@example.com");
        driver.findElement(By.id("com.nailvital.app:id/password_input")).sendKeys("badpass");
        driver.findElement(By.id("com.nailvital.app:id/login_button")).click();
        
        // Assert error message
        String errorMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("com.nailvital.app:id/error_message"))).getText();
        Assert.assertTrue(errorMsg.contains("Invalid credentials"), "Error message should contain 'Invalid credentials'");
    }

    @AfterMethod
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
