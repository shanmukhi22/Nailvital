const Page = require('./page');

class FormPage extends Page {
    // 1. Define the UI elements for the Registration Form
    get btnSignUpSwitch () { return $('a=Sign up'); }
    get inputName () { return $('#regName'); }
    get inputEmail () { return $('#regEmail'); }
    get inputPhone () { return $('#regPhone'); }
    get inputPassword () { return $('#regPass'); }
    get checkboxTerms () { return $('#regTermsCheck'); }
    get btnSubmit () { return $('button=Create Account'); }
    get otpScreenTitle () { return $('h1=Verify Email'); }

    // 2. Define an action using those elements
    async register (name, email, phone, password) {
        // First switch to the registration screen from the login screen
        await this.btnSignUpSwitch.click();
        
        await this.inputName.setValue(name);
        await this.inputEmail.setValue(email);
        await this.inputPhone.setValue(phone);
        await this.inputPassword.setValue(password);
        await this.checkboxTerms.click();
        await this.btnSubmit.click();
    }

    open () {
        return super.open('index.html');
    }
}
module.exports = new FormPage();
