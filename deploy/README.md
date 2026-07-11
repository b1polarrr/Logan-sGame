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

## 仅 Redis + MySQL（IDEA 本地跑游戏服）

```powershell
docker compose -f deploy/docker-compose.yml up -d redis mysql
```

- Redis：`localhost:6379`
- MySQL：`localhost:3306`，库名 `poker_aa`，用户 `poker` / `poker123`
- 账号表由 `deploy/mysql/init.sql` 在**首次**建库时自动执行

IDEA 运行 `PokerNettyServer` 时建议环境变量：

```
REDIS_URL=redis://localhost:6379
JDBC_URL=jdbc:mysql://localhost:3306/poker_aa?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
JDBC_USER=poker
JDBC_PASSWORD=poker123
```

验证表是否存在：

```powershell
docker exec -it poker_aa-mysql mysql -upoker -ppoker123 poker_aa -e "SHOW TABLES; DESCRIBE users;"
```

## Kubernetes（阶段 5）

详见 [k8s/README.md](k8s/README.md)。

```powershell
powershell -ExecutionPolicy Bypass -File deploy/k8s/scripts/build-images.ps1
kubectl apply -k deploy/k8s
# 访问 http://localhost:30080
```
