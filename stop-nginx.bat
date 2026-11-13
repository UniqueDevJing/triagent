@echo off
cd /d "%~dp0nginx-1.26.2"
echo 停止 Nginx...
nginx.exe -s stop
echo Nginx 已停止
pause
