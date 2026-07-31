class Page {
    async open(path) {
        await browser.url(path);
        
        // Skip Splash and Getting Started screens by directly manipulating the DOM
        await browser.execute(function() {
            var screens = document.querySelectorAll('.screen');
            for (var i = 0; i < screens.length; i++) {
                screens[i].classList.remove('active');
            }
            var loginScreen = document.getElementById('login');
            if (loginScreen) {
                loginScreen.classList.add('active');
            }
        });
    }
}
module.exports = Page;
