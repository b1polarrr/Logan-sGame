# 阶段 4 部署说明

> **构建失败 `auth.docker.io` 超时？** 国内网络请执行：
> `powershell -ExecutionPolicy Bypass -File deploy/scripts/build-compose-mirror.ps1`
> 详见 [DOCKER_MIRROR.md](DOCKER_MIRROR.md)。

## 全栈启动（Nginx + 2×game-server + Redis + Kafka）

在项目根目录执行：

```powershell
docker compose -f deploy/docker-compose.yml up -d --build
```

- 前端 + 入口：**http://localhost:8080**
- WebSocket：`ws://localhost:8080/ws`（Nginx `ip_hash` 粘性会话）
- Redis：`localhost:6379`
- Kafka：`localhost:9092`，主题默认 `poker.events`

查看 Kafka 事件（容器内）：

```powershell
docker exec -it poker_aa-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic poker.events --from-beginning
```

停止：

```powershell
docker compose -f deploy/docker-compose.yml down
```

## 仅 Redis（IDEA 本地跑游戏服）

```powershell
docker compose -f deploy/docker-compose.yml up -d redis
```

IDEA 运行 `PokerNettyServer`，`REDIS_URL=redis://localhost:6379`（默认即可）。

## Kubernetes（阶段 5）

详见 [k8s/README.md](k8s/README.md)。

```powershell
powershell -ExecutionPolicy Bypass -File deploy/k8s/scripts/build-images.ps1
kubectl apply -k deploy/k8s
# 访问 http://localhost:30080
```
