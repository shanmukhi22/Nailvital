const Page = require('./page');

class FormPage extends Page {
    // 1. Define the UI elements
    get inputName () { return $('#first-name-input'); }
    get inputEmail () { return $('#email-input'); }
    get btnSubmit () { return $('#submit-form-button'); }
    get successMessage () { return $('.success-alert'); }

    // 2. Define an action using those elements
    async fillAndSubmit (name, email) {
        await this.inputName.setValue(name);
        await this.inputEmail.setValue(email);
        await this.btnSubmit.click();
    }

    open () {
        return super.open('forms.html');
    }
}
module.exports = new FormPage();
