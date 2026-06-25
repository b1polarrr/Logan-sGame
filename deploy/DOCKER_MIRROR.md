# Docker 镜像拉取失败排错（国内网络）

报错类似：

```text
failed to fetch anonymous token: Get "https://auth.docker.io/token" ...
dial tcp ... connectex: A connection attempt failed
```

说明无法访问 Docker Hub，需要配置**镜像加速**。

## 方法一：Docker Desktop 镜像加速（推荐，一次配置全局生效）

1. 打开 **Docker Desktop** → **Settings** → **Docker Engine**
2. 在 JSON 中加入 `registry-mirrors`（保留原有其它配置）：

```json
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://docker.1panel.live"
  ]
}
```

3. 点击 **Apply & restart**
4. 重新构建：

```powershell
docker compose -f deploy/docker-compose.yml build --no-cache
```

或：

```powershell
powershell -ExecutionPolicy Bypass -File deploy/k8s/scripts/build-images.ps1
```

## 方法二：一键脚本（国内推荐）

```powershell
cd D:\resource\javatraining\Poker_AA
powershell -ExecutionPolicy Bypass -File deploy/scripts/build-compose-mirror.ps1
```

或只预拉基础镜像再普通构建：

```powershell
powershell -ExecutionPolicy Bypass -File deploy/scripts/pull-base-images.ps1
docker compose -f deploy/docker-compose.yml build
```

## 方法三：手动设置环境变量

```powershell
$env:DOCKER_REGISTRY_PREFIX = "docker.m.daocloud.io/library/"
powershell -ExecutionPolicy Bypass -File deploy/k8s/scripts/build-images.ps1
```

或 docker compose：

```powershell
$env:DOCKER_REGISTRY_PREFIX = "docker.m.daocloud.io/library/"
docker compose -f deploy/docker-compose.yml build
```

compose 会读取该环境变量作为 `REGISTRY_PREFIX` build-arg。

## 方法三：手动预拉取再构建

```powershell
docker pull docker.m.daocloud.io/library/node:20-alpine
docker tag docker.m.daocloud.io/library/node:20-alpine node:20-alpine
docker pull docker.m.daocloud.io/library/nginx:1.27-alpine
docker tag docker.m.daocloud.io/library/nginx:1.27-alpine nginx:1.27-alpine
docker pull docker.m.daocloud.io/library/maven:3.9-eclipse-temurin-17
docker tag docker.m.daocloud.io/library/maven:3.9-eclipse-temurin-17 maven:3.9-eclipse-temurin-17
docker pull docker.m.daocloud.io/library/eclipse-temurin:17-jre-alpine
docker tag docker.m.daocloud.io/library/eclipse-temurin:17-jre-alpine eclipse-temurin:17-jre-alpine
```

然后再执行正常 build。

## 验证

```powershell
docker pull node:20-alpine
```

能成功则说明镜像加速已生效。
