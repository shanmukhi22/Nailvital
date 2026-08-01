
exports.config = {
    runner: 'local',
    port: 4723,
    specs: [ '../tests/**/*.js' ],
    maxInstances: 1,
    capabilities: [{
        platformName: 'Android',
        'appium:deviceName': 'Nexus 6',
        'appium:automationName': 'UiAutomator2',
        'appium:app': '../App/app/build/outputs/apk/debug/app-debug.apk'
    }],
    logLevel: 'info',
    framework: 'mocha',
    reporters: ['spec'],
    mochaOpts: { ui: 'bdd', timeout: 60000 }
}
