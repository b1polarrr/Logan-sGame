# SOP：将 Poker_AA 代码更新到 GitHub

> 适用仓库：`https://github.com/b1polarrr/Logan-sGame.git`  
> 默认分支：`main`

---

## 一、前置条件（首次使用前检查）

| 项 | 说明 |
|---|---|
| GitHub 账号 | 已登录，对仓库有 push 权限 |
| Git 客户端 | 推荐安装 [Git for Windows](https://git-scm.com/download/win)，或使用 IDEA 内置 Git |
| 远程仓库 | 本项目已配置 `origin` → `Logan-sGame`，一般无需重复添加 |
| 编译通过 | push 前在 IDEA 执行 **Build → Rebuild Project**，确保 proto 已生成且无报错 |

---

## 二、标准流程（每次改完代码）

### 方式 A：IntelliJ IDEA（推荐）

1. **打开版本控制**
   - 菜单 `Git` → `Commit...`（或 `Ctrl + K`）

2. **查看变更**
   - 左侧勾选要提交的文件
   - 右侧填写 **Commit Message**（见下方示例）
   - 确认 diff 无多余文件（如 `target/`、`.idea/workspace.xml`）

3. **提交到本地**
   - 点击 **Commit**（仅本地）
   - 或 **Commit and Push**（本地 + 推送，一步到位）

4. **若只 Commit 了，还需 Push**
   - 菜单 `Git` → `Push...`（或 `Ctrl + Shift + K`）
   - 确认远程为 `origin/main`，点击 **Push**

5. **验证**
   - 浏览器打开：https://github.com/b1polarrr/Logan-sGame
   - 确认最新 commit 与提交信息一致

### 方式 B：命令行（Git 已安装且加入 PATH）

在项目根目录 `Poker_AA` 下执行：

```powershell
# 1. 查看状态
git status

# 2. 添加要提交的文件（或 git add . 添加全部，注意排除 target）
git add src/main/proto/game_protocol.proto
git add src/main/java/com/mercury/poker/engine/model/Player.java
git add src/main/java/com/mercury/poker/network/RoomRouter.java
git add src/main/java/com/mercury/poker/network/SnapshotBroadcaster.java
git add test.html

# 3. 提交
git commit -m "feat: 进房后需全员准备再开局"

# 4. 推送到 GitHub
git push origin main
```

---

## 三、Commit Message 规范（建议）

```
<type>: <简短说明>

<可选：详细说明>
```

| type | 含义 |
|---|---|
| `feat` | 新功能 |
| `fix` | 修复 bug |
| `refactor` | 重构（不改变行为） |
| `docs` | 文档 |
| `chore` | 构建、配置等杂项 |

**本次「准备开局」功能示例：**

```
feat: 进房后需全员准备再开局

- 新增 ActionType.READY 与 PlayerState.is_ready
- 坐下不再自动开局，全员准备后 startNewHand
- test.html 增加准备按钮
```

---

## 四、不要提交的文件

建议在项目根目录维护 `.gitignore`，至少忽略：

```
target/
.idea/workspace.xml
.idea/shelf/
*.iml
.DS_Store
```

> `target/` 为 Maven 编译产物；`.idea/workspace.xml` 为个人 IDE 状态，不应入库。

---

## 五、常见问题

### 1. Push 时要求登录 / 403

- **HTTPS**：GitHub 已不支持账号密码，需使用 [Personal Access Token (PAT)](https://github.com/settings/tokens) 作为密码
- **SSH**（可选）：生成密钥后，将公钥添加到 GitHub，并把 remote 改为 `git@github.com:b1polarrr/Logan-sGame.git`

### 2. Push 被拒绝：`Updates were rejected`

远程有你没有的 commit，先拉再推：

```powershell
git pull origin main --rebase
git push origin main
```

IDEA：`Git` → `Pull`，再 `Push`。

### 3. 冲突（Conflict）

- IDEA 会在编辑器中标记冲突块，手动选择保留内容后 **Mark as Resolved**
- 再 `Commit` → `Push`

### 4. proto 改完后编译报错

必须先 **Maven compile / Rebuild**，让 `game_protocol.proto` 生成新的 Java 类（如 `ActionType.READY`），再提交。

### 5. 误提交了 target/

```powershell
git rm -r --cached target/
git commit -m "chore: 停止跟踪 target 目录"
```

并在 `.gitignore` 中加入 `target/`。

---

## 六、快速检查清单（Push 前 30 秒）

- [ ] IDEA Rebuild 无报错
- [ ] Commit 未包含 `target/`、个人 IDE 配置
- [ ] Commit message 清晰
- [ ] Push 到 `origin/main`
- [ ] GitHub 网页上能看到最新提交

---

## 七、与本项目相关的远程信息

| 项 | 值 |
|---|---|
| 本地路径 | `d:\resource\javatraining\Poker_AA` |
| 远程名 | `origin` |
| 远程 URL | `https://github.com/b1polarrr/Logan-sGame.git` |
| 跟踪分支 | `main` → `origin/main` |
