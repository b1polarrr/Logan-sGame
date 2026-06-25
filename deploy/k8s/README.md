# 阶段 5：Kubernetes 部署

## 前置条件

- Kubernetes 集群（Docker Desktop K8s、Minikube、Kind 等）
- `kubectl`、`docker`
- 可选：Ingress Controller（Minikube: `minikube addons enable ingress`）

## 1. 构建镜像

在项目根目录：

```powershell
powershell -ExecutionPolicy Bypass -File deploy/k8s/scripts/build-images.ps1
```

Docker Desktop 使用本地镜像即可（`imagePullPolicy: IfNotPresent`）。

## 2. 部署全栈

```powershell
kubectl apply -k deploy/k8s
```

包含：Namespace、`redis`、`kafka`、game-server×2（HPA 2–4）、nginx、Ingress。

## 3. 访问

### 方式 A：NodePort（无需 Ingress）

```powershell
kubectl -n poker-aa get pods
# 浏览器访问 http://localhost:30080
```

Docker Desktop K8s 将 NodePort `30080` 映射到本机。

### 方式 B：Ingress + 域名

```powershell
minikube addons enable ingress
kubectl apply -k deploy/k8s
# 获取 Ingress IP
kubectl -n poker-aa get ingress poker-aa
```

将 `poker.local` 指向 Ingress IP，访问 `http://poker.local`。

### TLS（可选）

```powershell
openssl req -x509 -nodes -days 365 -newkey rsa:2048 `
  -keyout tls.key -out tls.crt -subj "/CN=poker.local"
kubectl -n poker-aa create secret tls poker-aa-tls --cert=tls.crt --key=tls.key
kubectl apply -f deploy/k8s/ingress-tls.yaml
```

访问 `https://poker.local`（需信任自签名证书）。

生产环境建议使用 [cert-manager](https://cert-manager.io/) + Let's Encrypt。

## 4. 验收

1. `http://localhost:30080` 打开大厅，创建房间、两人坐下对战。
2. HPA：`kubectl -n poker-aa get hpa`
3. 优雅下线：`kubectl -n poker-aa delete pod -l app=poker-aa-game-server --field-selector=status.phase=Running` 观察滚动更新与日志 draining。
4. Kafka 事件（Pod 内）：  
   `kubectl -n poker-aa exec deploy/poker-aa-kafka -- /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic poker.events --from-beginning`

## 5. 清理

```powershell
kubectl delete -k deploy/k8s
```

## 架构说明

| 组件 | 说明 |
|------|------|
| `poker-aa-nginx` | 静态前端 + `/ws` 反代（ConfigMap `ip_hash`） |
| `poker-aa-game-server` | Netty WS，`POD_NAME` 来自 Pod 名，SIGTERM 优雅下线 |
| Ingress | Cookie 粘性 `/ws` → game-server；`/` → nginx |
| HPA | game-server CPU 70%，副本 2–4 |

断线重连（阶段 3.7）在 Pod 切换时仍可恢复会话。
