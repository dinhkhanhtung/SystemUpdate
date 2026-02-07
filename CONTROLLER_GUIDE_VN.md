# 📱 Hướng Dẫn Controller - Theo Dõi Từ Điện Thoại

## 🎮 Giới Thiệu

**Controller** là chế độ cho phép bạn **từ điện thoại của mình** có thể theo dõi và điều khiển các **target phones** (những chiếc điện thoại khác đã cài AhMyth).

```
┌─ Device A: Bạn (người theo dõi)
│  └─ Cài app → Click "🎮 Controller"
│     └─ Kết nối tới AhMyth-Server
│        └─ Xem danh sách devices
│           └─ Chọn Device B, C, D... để điều khiển
│
├─ Device B: Target (bị theo dõi)
│  └─ Cài APK → Service chạy ngầm
│
└─ AhMyth-Server: Chạy trên Máy Tính
   └─ Nhận kết nối từ B, C, D...
   └─ Gửi lệnh từ A đến B, C, D...
```

---

## 🚀 Cách Sử Dụng Controller

### **Bước 1: Cài APK Trên Điện Thoại Của Bạn**
- Download: `AndroidSystem.apk`
- Cài đặt như bình thường
- Cấp quyền → Icon ẩn (bình thường)

### **Bước 2: Mở Controller**
- Click icon (icon sẽ hiện lại nhất thời)
- Hoặc: Click button "🎮 Controller - Điều Khiển Từ ĐT"

### **Bước 3: Nhập Server URL**
**Nếu cùng mạng WiFi (LAN):**
```
192.168.1.x:42474

Ví dụ: 192.168.1.100:42474
```

**Nếu từ ngoài (Remote/Ngrok):**
```
https://abc-xyz.ngrok-free.app:443

Hoặc: abc-xyz.ngrok-free.app:443
```

### **Bước 4: Click "🔗 Connect to Server"**
- App sẽ tải Dashboard từ server
- Chờ 2-3 giây

### **Bước 5: Xem Dashboard**
- Danh sách devices đã cài AhMyth
- Click vào device muốn điều khiển
- Chọn tính năng (Camera, Mic, Location, SMS, v.v.)

---

## 💡 Lợi Ích Controller

| Tính Năng | Chi Tiết |
|----------|---------|
| **Từ Điện Thoại** | Bạn có thể từ một chiếc điện thoại bình thường (không cần máy tính) |
| **Điều Khiển Nhiều Device** | Tất cả các devices cài AhMyth đều hiện trong danh sách |
| **Giống Web Dashboard** | Giao diện giống hệt web, nhưng trên Android |
| **Lưu URL** | Lần sau không cần nhập lại |
| **Flexible** | Đổi server URL bất kỳ lúc nào |
| **Port 42474 = LAN** | Nhanh nhất (trong mạng nhà) |
| **Port 443 = Ngrok** | Được dùng ở ngoài (chậm hơn nhưng công khai) |

---

## ⚙️ Cấu Hình Chi Tiết

### **URL Server - 3 Cách Nhập**

```
❌ KHÔNG HIỆU: 192.168.1.100
✅ CÓ HIỆU: 192.168.1.100:42474
✅ CÓ HIỆU: http://192.168.1.100:42474

❌ KHÔNG HIỆU: abc-xyz.ngrok-free.app
✅ CÓ HIỆU: abc-xyz.ngrok-free.app:443
✅ CÓ HIỆU: https://abc-xyz.ngrok-free.app:443
```

**App sẽ tự động thêm `http://` nếu bạn không ghi**

### **Lựa Chọn Server (LAN vs Remote)**

```
📡 LAN MODE (Nhanh - Nên dùng)
   192.168.1.100:42474
   Tốc độ: ⚡⚡⚡ (rất nhanh)
   Độ trễ: 0ms
   Yêu cầu: Cùng WiFi với target device
   Ưu điểm: Không cần ngrok, không bị chậm

🌍 REMOTE MODE (Chậm - Mặc định khi mất WiFi)
   https://abc-xyz.ngrok-free.app:443
   Tốc độ: ⚡⚡ (chậm hơn)
   Độ trễ: 500-1000ms
   Yêu cầu: Internet (WiFi hoặc Mobile)
   Ưu điểm: Có thể ở bất kỳ đâu trên thế giới
```

---

## 🔍 Chi Tiết Dashboard

### **Sau Khi Kết Nối**
Bạn sẽ thấy giao diện giống như máy tính, gồm:

```
┌─ SELECT DEVICE
│  ├─ Device 1 (Samsung)
│  ├─ Device 2 (iPhone mượn)
│  └─ Device 3 (Xiaomi)
│
├─ CAMERA
│  ├─ Front Camera (ảnh chân dung)
│  └─ Back Camera (ảnh phía sau)
│
├─ MICROPHONE
│  └─ Record Audio (ghi âm, lưu mp3)
│
├─ LOCATION
│  └─ GPS (lấy vị trí)
│
├─ SMS/CALL
│  ├─ SMS (xem tin nhắn)
│  └─ Call Log (xem lịch gọi)
│
├─ FILES
│  └─ Browse (duyệt tập tin, download)
│
└─ CONTACTS
   └─ List (xem danh bạ)
```

---

## 🔧 Troubleshooting

### **❌ "Connection failed"**
```
Lý do: Server không được bật hoặc URL sai
Cách Fix:
  1. Kiểm tra: AhMyth-Server đang chạy?
     npm start
  
  2. Kiểm tra: URL có đúng không?
     - LAN: ifconfig / ipconfig → lấy IP
     - Remote: Mở ngrok dashboard → copy URL
  
  3. Kiểm tra: WiFi có chung mạng?
     - Ping: ping 192.168.1.x
  
  4. Thử lại: Click "Reload" button
```

### **❌ "Webpage cannot be displayed"**
```
Nguyên nhân: WebView timeout
Cách Fix:
  1. Mở WiFi mạnh hơn
  2. Thử lại sau 5 giây
  3. Nếu LAN, chuyển sang Remote
```

### **❌ Device list trống**
```
Lý do: Không có device nào đã kết nối
Cách Fix:
  1. Cài APK trên target device
  2. Cấp quyền (Allow All)
  3. Chờ service khởi động
  4. Refresh dashboard (F5 hoặc pull refresh)
```

---

## 📊 Ví Dụ Thực Tế

### **Kịch Bản 1: Theo Dõi Business (Từ Máy Tính)**
```
Your Computer               Your Phone (Controller)      Target Phone (Spy)
    ↓                              ↓                             ↓
PC Browser          →    Android App (Controller)    ←    Service (Running)
  AhMyth                 • IP: 192.168.1.100             • Hidden
  Dashboard              • Port: 42474                   • Silent
                         • Connected: YES ✅              • Recording
    ├─ Camera            
    ├─ Location          
    ├─ SMS               
    └─ ...               
```

### **Kịch Bản 2: Remote Monitoring (Từ Ngoài Nhà)**
```
Your Phone (Controller)           Ngrok Server              Target Phone
  ├─ Open Controller        →    (Relay Traffic)      ←    Service
  ├─ URL: ngrok-free.app    →    192.168.x.x         ←    auto-connect
  ├─ Connect ✅             →    443 (HTTPS)          ←    location, camera
  └─ View Dashboard         →    Encrypt/Decrypt     ←    SMS read
      ├─ Camera                  
      ├─ GPS                     
      ├─ Contact                 
      └─ ...                    
```

---

## 🔒 Security Notes

### **Tại Sao Dùng HTTPS/Ngrok?**
```
LAN (192.168.1.x):
  - Gửi dữ liệu PLAIN TEXT
  - Không mã hóa
  - ⚠️ Nguy hiểm nếu mạng công cộng
  - ✅ An toàn nếu WiFi nhà riêng

HTTPS/Ngrok:
  - Mã hóa SSL/TLS
  - Không ai có thể đọc traffic
  - ✅ An toàn ở bất kỳ WiFi nào
  - ⚠️ Chậm hơn vì route qua ngrok
```

### **Best Practices**
```
✅ LAN:
   - Chỉ dùng trong nhà
   - Nhanh (0ms)
   - Không bị chặn

✅ HTTPS/Ngrok:
   - Dùng ngoài nhà
   - Bảo mật hơn
   - Chấp nhận network chậm

❌ TRÁNH:
   - Http (không mã hóa) qua public WiFi
   - Chia sẻ URL với người khác
   - Để URL trong chat/email
```

---

## 📝 Lưu Ý Quan Trọng

1. **Icon Có Thể Hiện Lại** -当你 click "Controller" button, icon sẽ hiện tạm thời để bạn điều hướng. Đó là bình thường.

2. **Service Luôn Chạy** - Ngay cả khi bạn đóng app, service vẫn chạy ở target device.

3. **URL Được Lưu** - Lần sắp tới, bạn không cần nhập lại URL.

4. **Ngrok URL Đổi** - Mỗi khi restart ngrok, URL thay đổi. Cập nhật URL mới vào Controller.

5. **Cùng Mạng = Nhanh** - LAN mode chỉ hoạt động khi cùng WiFi. Nếu device ra ngoài, tự động chuyển Remote.

---

## 🎯 Tóm Tắt

| Yếu Tố | Chi Tiết |
|--------|---------|
| **Cài Ở Đâu?** | Điện thoại của bạn (người theo dõi) |
| **Cách Mở?** | Click "🎮 Controller" từ MainActivity |
| **Nhập Gì?** | URL của AhMyth-Server (LAN hoặc Remote) |
| **Thấy Gì?** | Dashboard web - giống giao diện máy tính |
| **Điều Khiển Gì?** | Camera, Mic, Location, SMS, Files, Contacts |
| **Mất Kết Nối?** | Kiểm tra URL, server có chạy, WiFi có bật |
| **Lần Sau?** | URL được lưu, chỉ cần click "Reload" |

---

## 🚀 Bước Tiếp Theo

1. ✅ **Cài APK trên device của bạn** (người theo dõi)
2. ✅ **Cài APK trên target device** (bị theo dõi)
3. ✅ **Bật AhMyth-Server** (npm start)
4. ✅ **Click "🎮 Controller"**
5. ✅ **Nhập Server URL**
6. ✅ **Xem Dashboard + Điều khiển**

Enjoy! 🎉
