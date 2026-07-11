# 排错：ROOM_ON_OTHER_POD 无法加入房间

> 记录时间：2026-06-27  
> 相关 commit：`81ae246`（解决跨域问题 / 单机 compose 修复）

---

## 一、现象

前端日志反复出现：

```text
[错误] ROOM_ON_OTHER_POD 房间在另一台服务器（game-server-1）上，请刷新页面重试
[已发送] JOIN_ROOM room=148253 seat=0 amount=0
```

- 点击「加入房间」或自动进房失败
- 刷新页面、多次重试仍无效
- 阿里云上 `git pull` 显示 `Already up to date`，但问题依旧

---

## 二、根因

### 2.1 架构限制

| 组件 | 行为 |
|------|------|
| **game-server** | 房间状态在 **JVM 内存**（`RoomRouter.activeRoom`） |
| **Redis** | 只存房间**元数据**（含 `podId` 字段） |
| **Nginx** | 将 `/ws` 反代到 upstream 中的某一台 game-server |

旧版 `docker-compose` 默认启动 **两台** game-server（`game-server-1`、`game-server-2`），Nginx 使用 **`ip_hash`** 按客户端 IP 分配 Pod。

### 2.2 失败链路

1. 玩家 A（IP → Pod-1）创建房间 `148253`，内存与 Redis 中 `podId=game-server-1`
2. 玩家 B（IP → Pod-2）或同一玩家被分到 Pod-2 时发送 `JOIN_ROOM`
3. Pod-2 内存中无该房间 → 走 `handleMissingRoomJoin`
4. Redis 查到房间在 `game-server-1` → 返回错误码 **`ROOM_ON_OTHER_POD`**

### 2.3 为什么「刷新页面」无效

- Nginx `ip_hash` 对**同一 IP 固定**落到同一 Pod
- 刷新只会重建 WebSocket，**不会**把连接切到房间所在的 Pod
- 当前版本**未实现**跨 Pod 进房 / 房间迁移

### 2.4 为什么大厅能看到进不去的房间

旧版 `handleListRooms` 从 Redis 列出**所有**房间，包含其他 Pod 上的房间。  
玩家点击加入 → 必失败。

---

## 三、修复方案（已落地）

### 3.1 部署层：单机 compose（推荐阿里云测试环境）

**`deploy/docker-compose.yml`**

- 默认只启动 **`game-server-1`**
- `game-server-2` 改为可选 profile：`--profile multi-pod`（仅多 Pod 压测时用）
- Nginx 只依赖 `game-server-1`

**`deploy/nginx/nginx.conf`**

- upstream 只保留 `game-server-1:8888`
- 去掉 `ip_hash` 与 `game-server-2`

### 3.2 后端：房间列表只显示本 Pod 可加入的房间

**`RoomRouter.handleListRooms`**

- 仅当 `activeRoom` 中存在该房间时才加入列表
- 不再把 Redis 里其他 Pod / 已失效的「幽灵房间」展示给玩家

### 3.3 前端：错误时回大厅

**`frontend/src/composables/useGameSocket.ts`**

- 收到 `ROOM_ON_OTHER_POD` 或 `ROOM_NOT_FOUND` 时：
  - 清除本地 roomId / seatIndex
  - 回到大厅并刷新房间列表
  - 日志提示：加入列表中可见的房间或新建房间

---

## 四、阿里云部署步骤

修复代码 push 到 GitHub 后，在服务器执行：

```bash
cd ~/Logan-sGame
git pull origin main

# 确认最新 commit（应含单机 compose 修复）
git log -1 --oneline

# 停掉并删除旧的第二台 game-server（若曾启动过）
docker compose -f deploy/docker-compose.yml stop game-server-2
docker compose -f deploy/docker-compose.yml rm -f game-server-2

# 重建 nginx + 后端（nginx 配置变更必须 rebuild）
docker compose -f deploy/docker-compose.yml up -d --build nginx game-server-1
```

验证：

```bash
docker ps
```

应只有 **`poker_aa-game-1`**，不应再有 `poker_aa-game-2`。

浏览器：**Ctrl+F5** → 刷新房间列表 → **新建房间**再测试（旧房间号可能已在 Redis 残留且不可进）。

---

## 五、常见误区

### `git pull` 显示 Already up to date

| 含义 | 不代表 |
|------|--------|
| 服务器代码已与 **GitHub 远程** 一致 | 本机未 push 的 commit 已在服务器上 |
| | Docker 镜像已 rebuild |
| | 旧容器 game-server-2 已停止 |

正确顺序：**本机 cmd push → 阿里云 pull（应出现 Fast-forward）→ docker rebuild**。

### 本机 push 失败（GitHub 443 连不上）

`ping github.com` 通不代表 HTTPS 可用。可选：

1. 配置 Git 代理或换网络后再 `git push origin main`
2. 本机 `git bundle create` → 传到阿里云 → `git pull xxx.bundle main` → 在阿里云 `git push origin main`

---

## 六、相关代码位置

| 文件 | 说明 |
|------|------|
| `RoomRouter.handleMissingRoomJoin` | 返回 `ROOM_ON_OTHER_POD` |
| `RoomRouter.handleListRooms` | 房间列表过滤 |
| `RedisRoomRegistry.registerRoom` | 写入 `podId` |
| `deploy/docker-compose.yml` | 单机 / multi-pod profile |
| `deploy/nginx/nginx.conf` | WebSocket upstream |

---

## 七、后续（多 Pod 正式环境）

若 K8s / 多副本生产部署需要跨节点进房，需额外设计（当前 **未实现**）：

- 房间状态外置（Redis 存牌桌）或
- 按 `roomId` 路由到固定 Pod 或
- 跨 Pod 消息转发

阶段 4 的 docker-compose **单机模式**仅用于阿里云联调与小规格 ECS，避免误踩此问题。

---

## 八、文档索引

| 文档 | 说明 |
|------|------|
| [SOP-GitHub更新.md](./SOP-GitHub更新.md) | push / pull / rebuild 流程 |
| [开发约定.md](./开发约定.md) | 测试走 GitHub → 阿里云 |
