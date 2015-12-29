import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class InstallAppFromGooglePlayStore {

    private AppiumDriver driver;
    private WebDriverWait wait;
    private long explicitWaitTimeoutInSeconds = 10L;
    private static long INSTALL_DURATION_IN_SECONDS = 60L;

    // app to install - details
    final String testAppName = "Chromecast";
    final String testAppPackage = "com.google.android.apps.chromecast.app";
    final String testAppActivity = ".DiscoveryActivity";

    @Before
    public void setUp() throws Exception {

        DesiredCapabilities capabilities = DesiredCapabilities.android();
        capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "Samsung Galaxy s4");
        capabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, "5.0.1");
        capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
        capabilities.setCapability(MobileCapabilityType.BROWSER_NAME, "");
        capabilities.setCapability(MobileCapabilityType.APP_PACKAGE, "com.android.vending");
        capabilities.setCapability(MobileCapabilityType.APP_ACTIVITY, ".AssetBrowserActivity");
        capabilities.setCapability(MobileCapabilityType.APP_WAIT_ACTIVITY, ".AssetBrowserActivity");
        capabilities.setCapability(MobileCapabilityType.DEVICE_READY_TIMEOUT, 40);
        capabilities.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 180);
        capabilities.setCapability(MobileCapabilityType.APPIUM_VERSION, "1.4.16");
        capabilities.setCapability("deviceOrientation", "portrait");

        this.driver = new AndroidDriver(new URL("http://0.0.0.0:4723/wd/hub")
                , capabilities);

        this.wait = new WebDriverWait(driver, explicitWaitTimeoutInSeconds);

        uninstallApp(testAppPackage);
    }

    @After
    public void tearDown() throws Exception {
        driver.quit();
    }

    @Test
    public void installAppFromGooglePlayStore() throws Exception {
        // wait until search bar is visible, and then tap on it
        wait.until(ExpectedConditions.visibilityOf(
                driver.findElement(MobileBy.AndroidUIAutomator("new UiSelector().resourceId(\"com.android.vending:id/search_box_idle_text\")"))))
                .click();

        // type in the name of the app into the search bar
        driver.findElement(MobileBy.className("android.widget.EditText"))
                .sendKeys(testAppName);

        // tap on the suggested option that contains the app name
        // im using lowercase because of Google's design choice - they list all suggestions in lower case
        wait.until(ExpectedConditions.visibilityOf(
                driver.findElement(MobileBy.AndroidUIAutomator("new UiSelector().resourceId(\"com.android.vending:id/suggest_text\").text(\"" + testAppName.toLowerCase() + "\")"))))
                .click();

        // wait for the app title to be displayed
        wait.until(ExpectedConditions.visibilityOf(
                driver.findElement(MobileBy.AndroidUIAutomator("new UiSelector().resourceId(\"com.android.vending:id/li_title\").text(\"" + testAppName + "\")"))));

        // tap on the triple dot icon located on the app's tile
        driver.findElement(MobileBy.xpath("//android.widget.TextView[@content-desc=\"App: " + testAppName + "\"]/following-sibling::android.widget.ImageView[@resource-id=\"com.android.vending:id/li_overflow\"]"))
                .click();

        // tap on the Install button
        driver.findElement(MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.TextView\").resourceId(\"com.android.vending:id/title\").text(\"Install\")"))
                .click();

        // tap on accept
        driver.findElement(MobileBy.AndroidUIAutomator("new UiSelector().resourceId(\"com.android.vending:id/continue_button\")"))
                .click();

        // wait until "installed" shows up for INSTALL_DURATION_IN_SECONDS
        new WebDriverWait(driver, INSTALL_DURATION_IN_SECONDS).until(ExpectedConditions.presenceOfElementLocated(
                MobileBy.xpath("//android.widget.TextView[@content-desc=\"App: " + testAppName + "\"]/following-sibling::android.view.View[@resource-id=\"com.android.vending:id/li_label\"][@content-desc=\"Installed\"]")));

        // quit current driver instance - this quits the google playstore
        // and allows us to prepare for next stage - starting up the freshly installed app
        driver.quit();

        // launch newly installed app
        driver = new AndroidDriver(new URL("http://0.0.0.0:4723/wd/hub"), installedAppCaps());
        driver.launchApp();
        // wait until app loads and an element that should 100% be there is displayed
        // in this case, we're waiting for Google's "privacy and terms"
        WebElement alertTitle = wait.until(ExpectedConditions.visibilityOf(driver.findElement(MobileBy.AndroidUIAutomator("new UiSelector().resourceId(\"android:id/alertTitle\")"))));
        assertThat(alertTitle.getText(), is("Privacy and terms"));
    }

    // set up capabilities for the test app
    // these are required to start the app you install via playstore
    private DesiredCapabilities installedAppCaps() throws Exception {

        DesiredCapabilities capabilities = DesiredCapabilities.android();
        capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "Samsung Galaxy s4");
        capabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, "5.0.1");
        capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
        capabilities.setCapability(MobileCapabilityType.BROWSER_NAME, "");
        capabilities.setCapability(MobileCapabilityType.APP_PACKAGE, testAppPackage);
        capabilities.setCapability(MobileCapabilityType.APP_ACTIVITY, testAppActivity);
        capabilities.setCapability(MobileCapabilityType.APP_WAIT_ACTIVITY, testAppActivity);
        capabilities.setCapability(MobileCapabilityType.DEVICE_READY_TIMEOUT, 40);
        capabilities.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 180);
        capabilities.setCapability(MobileCapabilityType.APPIUM_VERSION, "1.4.16");
        capabilities.setCapability("deviceOrientation", "portrait");
        capabilities.setCapability("autoLaunch", "false");

        return capabilities;
    }

    //uninstall the app if it's already installed
    //credit where credit is due - code thanks to Craigo - http://stackoverflow.com/a/25735681

    private void uninstallApp(String appPackage) throws IOException, InterruptedException {
        final Process p = Runtime.getRuntime().exec("adb uninstall " + appPackage);

        new Thread(new Runnable() {
            public void run() {
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = null;

                try {
                    while ((line = input.readLine()) != null)
                        System.out.println(line);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        p.waitFor();
    }
}