@echo off
echo ========================================
echo 校园志愿服务管理平台 - 服务启动脚本
echo ========================================
echo.

echo [1/4] 启动监控服务 (monitor-service) - 端口: 9100
start "monitor-service" cmd /k "cd services\monitor-service && set APP_LOG_FILE=../../monitor-service/logs/debug.log && mvn spring-boot:run"
timeout /t 15 /nobreak

echo [2/4] 启动网关服务 (gateway-service) - 端口: 9000
start "gateway-service" cmd /k "cd services\gateway-service && set APP_LOG_FILE=../../gateway-service/logs/debug.log && mvn spring-boot:run"
timeout /t 15 /nobreak

echo [3/4] 启动用户服务 (user-service) - 端口: 8100
start "user-service" cmd /k "cd services\user-service && set APP_LOG_FILE=../../user-service/logs/debug.log && mvn spring-boot:run"
timeout /t 15 /nobreak

echo [4/4] 启动活动服务 (activity-service) - 端口: 8200
start "activity-service" cmd /k "cd services\activity-service && set APP_LOG_FILE=../../activity-service/logs/debug.log && mvn spring-boot:run"

echo.
echo ========================================
echo 所有服务启动命令已执行！
echo 请等待各服务完全启动（约1-2分钟）
echo.
echo 服务地址：
echo   - 监控中心: http://localhost:9100
echo   - API网关: http://localhost:9000
echo   - Nacos控制台: http://localhost:8848/nacos
echo ========================================
pause
