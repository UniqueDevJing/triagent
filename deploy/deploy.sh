#!/bin/bash
set -e

echo "=========================================="
echo "  传智健康管理系统 - 一键部署脚本"
echo "=========================================="

# 检查 .env
if [ ! -f .env ]; then
    echo "[错误] 请先创建 .env 文件: cp .env.example .env"
    echo "       然后编辑 .env 填入真实密码和密钥"
    exit 1
fi

# 安装 Docker (如未安装)
if ! command -v docker &> /dev/null; then
    echo "[安装] Docker..."
    curl -fsSL https://get.docker.com | bash
fi

# 安装 Docker Compose (如未安装)
if ! command -v docker compose &> /dev/null; then
    echo "[安装] Docker Compose..."
    apt-get update && apt-get install -y docker-compose-plugin
fi

# 启动服务
echo "[启动] 服务..."
docker compose up -d --build

# 等待启动
echo "[等待] 后端启动中..."
sleep 30

# 检查状态
echo ""
echo "=========================================="
echo "  服务状态"
echo "=========================================="
docker compose ps

echo ""
echo "=========================================="
echo "  部署完成！"
echo "=========================================="
echo "  前端: http://服务器IP"
echo "  后端: http://服务器IP:8080"
echo "  文档: http://服务器IP:8080/doc.html"
echo "  登录: admin / admin123"
echo ""
echo "  查看日志: docker compose logs -f backend"
echo "  停止服务: docker compose down"
