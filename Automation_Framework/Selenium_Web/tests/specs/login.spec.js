const LoginPage = require('../pageobjects/login.page');

describe('My Login application', () => {
    it('should login with valid credentials', async () => {
        await LoginPage.open();
        
        await LoginPage.login('testuser', 'SuperSecretPassword!');
        
        // Assert that URL changed or some authenticated element is visible
        const currentUrl = await browser.getUrl();
        expect(currentUrl).not.toContain('login');
    });

    it('should show error with invalid credentials', async () => {
        await LoginPage.open();
        
        await LoginPage.login('wronguser', 'wrongpass');
        
        // In a real app, you would assert an error message here.
        // const errorMsg = await $('#error-message');
        // await expect(errorMsg).toBeExisting();
        // await expect(errorMsg).toHaveTextContaining('Invalid credentials');
    });
});
