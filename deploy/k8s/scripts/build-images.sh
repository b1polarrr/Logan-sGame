#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"

echo "==> 构建 game-server 镜像..."
docker build -f "$ROOT/deploy/docker/Dockerfile.game-server" -t poker-aa/game-server:latest "$ROOT"

echo "==> 构建 nginx 镜像..."
docker build -f "$ROOT/deploy/nginx/Dockerfile" -t poker-aa/nginx:latest "$ROOT"

if command -v minikube >/dev/null 2>&1 && minikube status --format='{{.Host}}' 2>/dev/null | grep -q Running; then
  echo "==> 加载镜像到 Minikube..."
  minikube image load poker-aa/game-server:latest
  minikube image load poker-aa/nginx:latest
fi

echo "完成。镜像: poker-aa/game-server:latest, poker-aa/nginx:latest"
