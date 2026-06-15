@echo off
cd /d "%~dp0nginx-1.26.2"
echo 启动传智健康管理系统 Nginx...
nginx.exe
echo Nginx 已启动: http://localhost
pause
