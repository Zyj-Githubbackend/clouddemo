#!/bin/bash

echo "========================================"
echo "校园志愿服务管理平台 - 服务启动脚本"
echo "========================================"
echo ""

echo "[1/4] 启动监控服务 (monitor-service) - 端口: 9100"
cd services/monitor-service
nohup env APP_LOG_FILE=../../monitor-service/logs/debug.log mvn spring-boot:run > /dev/null 2>&1 &
cd ../..
sleep 15

echo "[2/4] 启动网关服务 (gateway-service) - 端口: 9000"
cd services/gateway-service
nohup env APP_LOG_FILE=../../gateway-service/logs/debug.log mvn spring-boot:run > /dev/null 2>&1 &
cd ../..
sleep 15

echo "[3/4] 启动用户服务 (user-service) - 端口: 8100"
cd services/user-service
nohup env APP_LOG_FILE=../../user-service/logs/debug.log mvn spring-boot:run > /dev/null 2>&1 &
cd ../..
sleep 15

echo "[4/4] 启动活动服务 (activity-service) - 端口: 8200"
cd services/activity-service
nohup env APP_LOG_FILE=../../activity-service/logs/debug.log mvn spring-boot:run > /dev/null 2>&1 &
cd ../..

echo ""
echo "========================================"
echo "所有服务启动命令已执行！"
echo "请等待各服务完全启动（约1-2分钟）"
echo ""
echo "服务地址："
echo "  - 监控中心: http://localhost:9100"
echo "  - API网关: http://localhost:9000"
echo "  - Nacos控制台: http://localhost:8848/nacos"
echo ""
echo "日志目录: ./logs/"
echo "========================================"
