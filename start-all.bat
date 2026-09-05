@echo off
chcp 65001 >nul
echo ========================================
echo   传智健康管理系统 - 启动所有服务
echo ========================================

:: 检查 MySQL 是否运行
echo [1/4] 检查 MySQL 服务...
sc query MySQL80 | find "RUNNING" >nul 2>&1
if %errorlevel% neq 0 (
    echo   启动 MySQL...
    net start MySQL80
) else (
    echo   MySQL 已在运行
)

:: 检查 Redis 是否运行
echo [2/4] 检查 Redis 服务...
sc query Redis | find "RUNNING" >nul 2>&1
if %errorlevel% neq 0 (
    echo   尝试启动 Redis...
    redis-server --service-start 2>nul || echo   Redis 未安装为服务，请手动启动
) else (
    echo   Redis 已在运行
)

:: 启动 Nginx
echo [3/4] 启动 Nginx...
cd /d "%~dp0"
if exist "nginx-1.26.2\nginx.exe" (
    start "Nginx-Health" "nginx-1.26.2\nginx.exe"
    echo   Nginx 启动完成
) else (
    echo   Nginx 未找到，跳过
)

:: 启动后端（新架构 health-admin）
echo [4/4] 启动后端服务...
cd /d "%~dp0"
start "Health-Admin" cmd /c "mvn -pl health-admin -am spring-boot:run"

echo.
echo ========================================
echo   所有服务启动完成!
echo   前端: http://localhost:3000
echo   后端: http://localhost:8080
echo   API文档: http://localhost:8080/doc.html
echo ========================================
pause
