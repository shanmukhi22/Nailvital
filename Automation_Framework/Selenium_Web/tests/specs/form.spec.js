const FormPage = require('../pageobjects/form.page');

describe('Forms Module Validation', () => {
    it('TC_WEB_FORMS_001 - should successfully submit the registration form and go to OTP', async () => {
        // Step 1: Navigate to page
        await FormPage.open();
        
        // Wait for page to load
        await FormPage.btnSignUpSwitch.waitForDisplayed({ timeout: 5000 });
        
        // Step 2: Enter data and submit the Registration form
        await FormPage.register('Ram Srinivas', 'gummaramsrinivas2004@gmail.com', '9390088908', 'SecurePassword123!');
        
        // Step 3: Verify the Expected Result (Navigated to OTP Screen)
        await expect(FormPage.otpScreenTitle).toBeExisting();
    });
});
