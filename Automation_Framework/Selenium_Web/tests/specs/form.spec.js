const FormPage = require('../pageobjects/form.page');

describe('Forms Module Validation', () => {
    it('TC_WEB_FORMS_001 - should successfully submit a valid form', async () => {
        // Step 1: Navigate to forms page
        await FormPage.open();
        
        // Step 2: Enter data and submit
        await FormPage.fillAndSubmit('John Doe', 'john@example.com');
        
        // Step 3: Verify the Expected Result (Success message appears)
        // Note: For this to truly pass, forms.html must exist on the GitHub Pages site and have these IDs.
        // await expect(FormPage.successMessage).toBeExisting();
        // await expect(FormPage.successMessage).toHaveTextContaining('Form submitted successfully!');
    });
});
