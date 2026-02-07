@echo off
REM AhMyth Server Launcher
REM Developer Mode - Starts server and shows terminal

cd /d "d:\Dev\Projects\Android\Android-RAT-master\SystemUpdate\AhMyth-Server"

echo.
echo ======================================
echo   AhMyth Server - Starting...
echo ======================================
echo.
echo Server will open in Electron window
echo Check terminal below for connection logs
echo.
echo Keep this window and the Electron window open!
echo.
pause

npm start

pause
