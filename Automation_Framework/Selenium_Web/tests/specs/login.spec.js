const LoginPage = require('../pageobjects/login.page');

describe('Login Authentication Verification', () => {
    it('TC_WEB_AUTH_001 - should login with valid credentials and show dashboard', async () => {
        await LoginPage.open();
        
        // Wait for login form to be visible (in case it takes time to render or display)
        await LoginPage.inputEmail.waitForDisplayed({ timeout: 5000 });
        
        // Fill form and click sign in
        await LoginPage.login('gummaramsrinivas2004@gmail.com', 'SecurePassword123!');
        
        // After clicking login, it should navigate to home and display the username
        // (Note: Since it's client-side JS, we just wait for the home screen element)
        await expect(LoginPage.homeUserName).toBeExisting();
    });
});
