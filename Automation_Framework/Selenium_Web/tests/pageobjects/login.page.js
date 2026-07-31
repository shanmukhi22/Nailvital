const Page = require('./page');

class LoginPage extends Page {
    get inputEmail () {
        return $('#loginEmail');
    }

    get inputPassword () {
        return $('#loginPass');
    }

    get btnSubmit () {
        return $('button=SIGN IN');
    }

    get homeUserName () {
        return $('#homeUserNameDisplay');
    }

    async login (email, password) {
        await this.inputEmail.setValue(email);
        await this.inputPassword.setValue(password);
        await this.btnSubmit.click();
    }

    open () {
        return super.open('index.html');
    }
}

module.exports = new LoginPage();
