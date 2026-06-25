# 构建并加载镜像到本地集群（Docker Desktop / Minikube）
# 在项目根目录执行：
#   powershell -ExecutionPolicy Bypass -File deploy/k8s/scripts/build-images.ps1
#
# 国内网络拉取 Docker Hub 失败时：
#   $env:DOCKER_REGISTRY_PREFIX = "docker.m.daocloud.io/library/"
#   powershell -ExecutionPolicy Bypass -File deploy/k8s/scripts/build-images.ps1
# 详见 deploy/DOCKER_MIRROR.md

$ErrorActionPreference = "Stop"
$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")

$RegistryPrefix = $env:DOCKER_REGISTRY_PREFIX
if ($null -eq $RegistryPrefix) {
    $RegistryPrefix = ""
}

$BuildArgs = @()
if ($RegistryPrefix -ne "") {
    Write-Host "使用镜像前缀: $RegistryPrefix"
    $BuildArgs = @("--build-arg", "REGISTRY_PREFIX=$RegistryPrefix")
}

Write-Host "==> 构建 game-server 镜像..."
docker build @BuildArgs -f "$Root\deploy\docker\Dockerfile.game-server" -t poker-aa/game-server:latest $Root

Write-Host "==> 构建 nginx 镜像..."
docker build @BuildArgs -f "$Root\deploy\nginx\Dockerfile" -t poker-aa/nginx:latest $Root

# Minikube 需加载镜像到集群
if (Get-Command minikube -ErrorAction SilentlyContinue) {
    $minikubeStatus = minikube status --format='{{.Host}}' 2>$null
    if ($minikubeStatus -eq "Running") {
        Write-Host "==> 加载镜像到 Minikube..."
        minikube image load poker-aa/game-server:latest
        minikube image load poker-aa/nginx:latest
    }
}

Write-Host "完成。镜像: poker-aa/game-server:latest, poker-aa/nginx:latest"
