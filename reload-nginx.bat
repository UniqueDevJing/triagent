@echo off
cd /d "%~dp0nginx-1.26.2"
echo 重载 Nginx 配置...
nginx.exe -s reload
echo 配置已重载
pause
