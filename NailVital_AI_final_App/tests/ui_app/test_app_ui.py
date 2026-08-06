import pytest
from appium import webdriver
from appium.options.android import UiAutomator2Options
from appium.webdriver.common.appiumby import AppiumBy

@pytest.fixture
def driver():
    options = UiAutomator2Options()
    options.platform_name = 'Android'
    
    # You may need to change these to match your emulator/device and app
    # options.device_name = 'emulator-5554'
    # options.app_package = 'com.example.nailvital'
    # options.app_activity = '.MainActivity'
    
    # Assuming appium is running on default port
    driver = webdriver.Remote('http://127.0.0.1:4723', options=options)
    driver.implicitly_wait(10)
    
    yield driver
    
    driver.quit()

def test_app_starts(driver):
    # This is a generic test just checking that the app connects.
    # Replace the locator with an actual element ID or XPath from your App
    # element = driver.find_element(AppiumBy.ID, 'com.example.nailvital:id/title')
    # assert element.is_displayed()
    pass
