@echo off
echo ========================================================
echo DANG DAY CODE LEN GITHUB: dinhkhanhtung/SystemUpdate
echo ========================================================
echo.
echo Neu duoc hoi, hay nhap Username va Password (hoac Token) GitHub cua ban.
echo.
git push -u origin main
echo.
if %ERRORLEVEL% EQU 0 (
    echo [THANH CONG] Code da duoc day len!
    echo Hay vao GitHub > Actions de xem no tu dong Build.
) else (
    echo [LOI] Khong the day len GitHub. Kiem tra lai mang hoac mat khau.
)
pause
