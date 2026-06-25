# 国内网络一键构建 compose 全栈（使用 DaoCloud 镜像前缀）
# 用法：powershell -ExecutionPolicy Bypass -File deploy/scripts/build-compose-mirror.ps1

$ErrorActionPreference = "Stop"
$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $Root

$env:DOCKER_REGISTRY_PREFIX = "docker.m.daocloud.io/library/"
Write-Host "DOCKER_REGISTRY_PREFIX=$env:DOCKER_REGISTRY_PREFIX"

docker compose -f deploy/docker-compose.yml build

Write-Host "Build finished. Start with:"
Write-Host "  docker compose -f deploy/docker-compose.yml up -d"
