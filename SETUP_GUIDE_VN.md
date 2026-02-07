# AhMyth - Hướng Dẫn Sử Dụng (Phiên Bản Thực Tế)

**Dành cho người dùng không chuyên - Đơn giản & Rõ ràng!**

---

## 🚀 **Bắt Đầu Lần Đầu Tiên**

### **Bước 1: Bấp để mở Server**
Bấp đúp file này (tùy chọn):
- **`START_SERVER.bat`** ← Hiện terminal (dễ thấy lỗi nếu có)
- **`START_SERVER.vbs`** ← Mở im lặng (nếu chỉ muốn dùng app)

**Kết quả:** Electron window "AhMyth Server" sẽ mở lên trong vòng 5-10 giây

---

### **Bước 2: Cài APK lên Điện Thoại**
File: **`AndroidSystem.apk`** (trong thư mục này)

**Cách cài:**
- Copy file vào điện thoại (USB hoặc cloud)
- Bấm vào APK → "Install"
- Cho phép cài từ nguồn không xác định (nếu được hỏi)

**Sau khi cài:**
- Ứng dụng **không hiện icon**
- Lần đầu tiên, bạn sẽ được hỏi cấp quyền
- Bấm **"Allow All"** để cho phép camera, vị trí, ghi âm, v.v.
- Service chạy ẩn trong nền
- Bạn sẽ thấy 1 notification "Android System"

⚠️ **Nếu không cấp quyền:** App sẽ không thể dùng camera, mic, vị trí → chức năng gồi hạn!

---

### **Bước 3: Kiểm Tra Kết Nối**
Trên máy tính, giao diện AhMyth Server:
1. Click tab **"Victims"**
2. Chờ 5-10 giây
3. Nếu thấy điện thoại trong danh sách → **Thành công!** ✅

---

## 🔄 **Khi Bạn Khởi Động Lại Server**

### **Vấn Đề:**
Mỗi lần restart server / ngrok, URL công khai thay đổi (nếu dùng ngrok free)

### **Giải Pháp (siêu đơn giản) - Dùng SettingsActivity (NO REBUILD!)**

**Cách 1: Cập nhật URL mà không buid lại APK (Khuyên Dùng)**

1. **Lấy URL ngrok mới:**
   - Bấp `START_SERVER.bat`
   - Mở trình duyệt → `http://127.0.0.1:4040`
   - Copy URL HTTPS (dòng đầu)
   - VD: `https://xxxxxx-xxxxx.ngrok-free.app`

2. **Update trong app (trên điện thoại):**
   - Bấp notification "Android System" ở system tray
   - Bấp nút **"Settings"** 
   - Nhập URL mới vào ô "Server Host"
   - Nhập Port: **443**
   - Bấm **"Save"**
   - App sẽ tự kết nối lại

**Lợi ích:** ⚡ Chỉ mất 10 giây, không cần cài APK lại!

---

### **Giải Pháp (Cách Cũ) - Build APK Mới**

Nếu muốn chắc chắn, bạn vẫn có thể build APK mới:

1. Ở giao diện AhMyth Server → Click tab **"APK Builder"**
2. Nhập thông tin:
   ```
   [Server Host] ← Dán URL ngrok từ bước 1 (VD: xxxxxx-xxxxx.ngrok.io)
   [Server Port] ← Nhập: 443
   [✓] Use HTTPS  ← Bấm để check
   ```
3. Bấm **"Build"** → chờ 2-3 phút
4. APK mới sẽ được tạo tại folder `outputs/`

---

## 🎯 **Nhanh Gọn - Bảng Tóm Tắt**

| Tình Huống | Làm Gì |
|-----------|--------|
| **Bắt đầu lần 1** | 1. Bấp `START_SERVER.bat` 2. Cài `AndroidSystem.apk` 3. Kiểm tra "Victims" |
| **Dùng trên Android (LAN)** | 1. Cài APK 2. Mở app → Settings 3. Nhập IP server local (192.168.1.x) 4. Click Dashboard |
| **Restart server** | Bấp `START_SERVER.bat` rồi dùng app để kết nối |

---

## 📱 **Dùng Dashboard trên Android (Mới!)**

### Lợi ích:
- ✅ Xem victim list trực tiếp trên điện thoại
- ✅ Khi cùng WiFi: **nhanh gấp 10x**, không cần ngrok
- ✅ Không cần cài đi cài lại APK

### Cách dùng:

**Bước 1: Cài APK**
- Cài `AndroidSystem.apk` như bình thường
- Cho phép quyền

**Bước 2: Cấu hình Server**
1. Mở app → bấm nút **"⚙️ Settings"**
2. Điền thông tin:
   ```
   [Server Host] → 0b00-2001-ee0-4a10-c500-c4a7-b2e-1d74-aa7e.ngrok-free.app
   [Server Port] → 443
   [LAN IP (Local Network)] → 192.168.1.2  ← IP máy tính trên WiFi
   ```
3. Bấm **"Save Settings"**

**Bước 3: Mở Dashboard**
- Bấm nút **"📊 Open Dashboard"**
- WebView sẽ load giao diện server
- Xem victim list, control device, etc

### ⚡ Khi cùng WiFi: Tự động dùng LAN IP (nhanh!)
- Nếu điền LAN IP → app sẽ ưu tiên dùng nó
- LAN không cần ngrok, nhanh và ổn định
- Nếu offline → tự fallback sang Remote host (ngrok)

---
| **Cập nhật cấu hình** | Không cần! APK mới tự động cập nhật |
| **Điện thoại mất kết nối** | Restart app hoặc khởi động lại máy tính (server cũng restart) |

---

## 🆘 **Nếu Gặp Vấn Đề**

### **P1: "Không thấy điện thoại trong Victims list"**
- Kiểm tra: Notification "Android System" có trên điện thoại không?
- Nếu không → APK chưa cài hoặc cài không thành công
- Nếu có → Chờ thêm 10-15 giây, ngrok có thể chậm

### **P2: "Phát hiện icon ứng dụng on screen"**
- Đó là hành động cố định - app đã loại bỏ icon sau cài
- Notification vẫn chạy ẩn ở background

### **P3: "Lỗi khi build APK"**
- Kiểm tra Server Host có đúng format không (VD: `xxxxx.ngrok.io`)
- Không được để dấu cách thừa

### **P4: "APK cài không được"**
- Cho phép cài app từ nguồn không xác định:
  - Android 12+: Settings → Apps → Special app access → Install unknown apps → chọn file manager
  - Android 10-11: Settings → Security → Unknown sources

---

## 📋 **File Quan Trọng**

```
d:\Dev\Projects\Android\Android-RAT-master\SystemUpdate\
├── START_SERVER.bat          ← Bấp để chạy server
├── AndroidSystem.apk         ← Cài lên điện thoại
└── SETUP_GUIDE_VN.md        ← Hướng dẫn này
```

---

## ✅ **Checklist Trước Khi Dùng**

- [ ] Máy tính kết nối internet (để chạy ngrok)
- [ ] Node.js đã cài (test: mở PowerShell → gõ `node --version`)
- [ ] Ngrok đã cấu hình auth token
- [ ] Điện thoại cùng mạng wifi hoặc internet (ngrok sẽ tunnel)

---

## 🎓 **Mẹo Nâng Cao** (tùy chọn)

**Nếu muốn URL cố định (không đổi khi restart):**
- Upgrade ngrok pro ($5-10/tháng) → được URL tĩnh
- Hoặc dùng duckdns.org (miễn phí) → tạo dynamic DNS

**Hiện tại (free ngrok):**
- URL thay đổi mỗi lần restart
- Nhưng chỉ cần build APK mới là được

---

**Thắc mắc gì cứ hỏi!** 💬
Hành trình của bạn bắt đầu đây! 🚀
