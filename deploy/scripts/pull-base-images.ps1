# 从 DaoCloud 镜像站拉取并 tag 为 Docker Hub 同名镜像（国内网络）
# 用法：powershell -ExecutionPolicy Bypass -File deploy/scripts/pull-base-images.ps1

$ErrorActionPreference = "Stop"
$Mirror = "docker.m.daocloud.io/library"

$LibraryImages = @(
    "node:20-alpine",
    "nginx:1.27-alpine",
    "maven:3.9-eclipse-temurin-17",
    "eclipse-temurin:17-jre-alpine",
    "redis:7-alpine"
)

foreach ($image in $LibraryImages) {
    $mirrorImage = "$Mirror/$image"
    Write-Host "Pull $mirrorImage ..."
    docker pull $mirrorImage
    docker tag $mirrorImage $image
    Write-Host "Tagged as $image"
}

$kafkaMirror = "docker.m.daocloud.io/apache/kafka:3.7.0"
Write-Host "Pull $kafkaMirror ..."
docker pull $kafkaMirror
docker tag $kafkaMirror apache/kafka:3.7.0
Write-Host "Tagged as apache/kafka:3.7.0"

Write-Host "Done. You can now build without DOCKER_REGISTRY_PREFIX if using standard image names."
