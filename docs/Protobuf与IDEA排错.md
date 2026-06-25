# Protobuf 生成类找不到 / IDEA 标红排错指南

> 现象：`com.mercury.poker.network.protocol` 整包标红，提示「程序包不存在」；  
> 或 `PlayerActionRequest`、`GameType`、`RoomInfo` 等无法解析。  
> 本文说明原因与标准处理流程。

---

## 一、原因说明

本项目的 Protobuf Java 类 **不是手写的**，由 Maven 从 `src/main/proto/game_protocol.proto` **自动生成**，输出目录为：

```text
target/generated-sources/protobuf/java/com/mercury/poker/network/protocol/
```

常见标红原因：

| 原因 | 说明 |
|------|------|
| **未执行 generate-sources** | 只点了 compile，proto 还没生成 |
| **clean 后未重新生成** | `target` 被清空，生成类消失 |
| **IDEA 未识别生成目录** | 目录存在，但未标记为 Generated Sources |
| **Poker_AA.iml 损坏** | 模块只认 `target/classes`，不认 `src/main/java` 和 proto 输出 |
| **整个 target 被 Exclude** | 且未单独把 `generated-sources/protobuf/java` 标为源码 |

**注意：** `engine.redis` 包下的类 **不应** import `network.protocol`（分层隔离）。若 Redis 类引用了 protocol，应改用 `LobbyRoomMeta` 等纯 Java 类型。

---

## 二、标准修复流程（按顺序做）

### 步骤 1：生成 Protobuf Java 代码

任选一种方式：

**方式 A — 双击脚本（推荐）**

```text
scripts\generate-proto.bat
```

**方式 B — IDEA Maven 面板**

1. 右侧 **Maven** → **Lifecycle**
2. 双击 **`generate-sources`**
3. 再双击 **`compile`**

**方式 C — 命令行（IDEA 自带 Maven）**

```powershell
"D:\APPS\IntelliJ IDEA 2022.3.3\plugins\maven\lib\maven3\bin\mvn.cmd" -f pom.xml clean generate-sources compile -DskipTests
```

（`MAVEN_CMD` 路径按本机 IDEA 安装位置修改。）

### 步骤 2：确认文件已生成

在资源管理器中应存在：

```text
target\generated-sources\protobuf\java\com\mercury\poker\network\protocol\GameType.java
```

或在 PowerShell：

```powershell
Test-Path "target\generated-sources\protobuf\java\com\mercury\poker\network\protocol\GameType.java"
```

应返回 `True`。

### 步骤 3：IDEA 模块与源码目录

1. **File → Project Structure → Modules → Poker_AA → Sources**
2. 确认蓝色 **Sources**：
   - `src\main\java`
   - `target\generated-sources\protobuf\java`
3. **Excluded Folders** 中：
   - 可以保留 `target`，但 **必须** 同时把 `target\generated-sources\protobuf\java` 标为 Sources（已在 `Poker_AA.iml` 中配置）
   - **不要** 只留 `target\classes` 作为唯一源码目录

### 步骤 4：Reload Maven

Maven 面板 → 点击 **Reload All Maven Projects**（刷新图标）。

### 步骤 5：仍标红时

**File → Invalidate Caches → Invalidate and Restart**

---

## 三、Maven 面板找不到 protobuf 插件？

插件全名是 **`protobuf-maven-plugin`**，在 **Plugins** 下展开可见；若没有：

- 直接跑 **Lifecycle → generate-sources**（会自动调用 protobuf 插件）
- 或 **Execute Maven Goal** 输入：`protobuf:compile`

---

## 四、修改 proto 文件之后

每次改 `src/main/proto/game_protocol.proto` 后必须：

1. **generate-sources**（或 `generate-proto.bat`）
2. **compile**
3. 前端 `frontend/src/proto/gameProtocol.ts` 里的 `PROTO` 字符串需 **手动同步**（若联调页依赖同一协议）

---

## 五、Redis Session Key 约定

Session 在 Redis 中的 key 格式为：

```text
session:{token}
```

例如 token 为 `94cbc3fb...` 时，key 为：

```text
session:94cbc3fb53ab4db5a11cda5663448699
```

查询示例：

```powershell
docker exec poker_aa-redis redis-cli KEYS "session:*"
docker exec poker_aa-redis redis-cli HGETALL session:你的token
```

若 `KEYS "session:*"` 无输出，检查：

- 是否已启动 `PokerNettyServer` 且前端已 **连接**
- 是否使用了 **IDEA 控制台最新** 的 `sessionToken`（不要用旧 token）

---

## 六、快速自检清单

- [ ] `GameType.java` 存在于 `target/generated-sources/protobuf/java/...`
- [ ] Maven **BUILD SUCCESS**
- [ ] `Poker_AA.iml` 含 `src/main/java` 与 `generated-sources/protobuf/java`
- [ ] Maven **Reload** 已执行
- [ ] `RoomRouter` 等 network 类不再标红

---

## 七、相关文件

| 文件 | 作用 |
|------|------|
| `src/main/proto/game_protocol.proto` | 协议定义 |
| `pom.xml` | `protobuf-maven-plugin` + `build-helper-maven-plugin` |
| `scripts/generate-proto.bat` | 一键 generate-sources + compile |
| `Poker_AA.iml` | IDEA 模块源码根配置 |

---

*最后更新：修复 sessionKey 冒号、重建 Poker_AA.iml 源码根*
