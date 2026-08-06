import pytest
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

@pytest.fixture
def driver():
    options = webdriver.ChromeOptions()
    # options.add_argument('--headless') # Uncomment for headless mode
    driver = webdriver.Chrome(options=options)
    driver.implicitly_wait(10)
    yield driver
    driver.quit()

def test_website_loads_successfully(driver):
    # Adjust this URL to wherever the frontend is running
    driver.get("http://localhost:3000")
    
    # Check if a specific element is present, e.g., body or main header
    WebDriverWait(driver, 10).until(
        EC.presence_of_element_located((By.TAG_NAME, "body"))
    )
    
    # Assert something basic
    assert "Nail" in driver.title or driver.title != "", "Page title should not be empty"
