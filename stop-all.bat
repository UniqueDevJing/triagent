@echo off
chcp 65001 >nul
echo ========================================
echo   传智健康管理系统 - 停止所有服务
echo ========================================

echo [1/3] 停止 Nginx...
taskkill /f /im nginx.exe 2>nul && echo   Nginx 已停止 || echo   Nginx 未运行

echo [2/3] 停止后端服务...
taskkill /f /fi "WINDOWTITLE eq Health-Server*" 2>nul
:: 停止占用8080端口的Java进程
for /f "tokens=5" %%a in ('netstat -ano ^| find ":8080" ^| find "LISTENING" 2^>nul') do (
    taskkill /f /pid %%a 2>nul
)
echo   后端服务已停止

echo [3/3] 清理临时文件...
cd /d "%~dp0"
if exist "health-server\target\tmp" rmdir /s /q "health-server\target\tmp" 2>nul

echo.
echo ========================================
echo   所有服务已停止
echo ========================================
pause
