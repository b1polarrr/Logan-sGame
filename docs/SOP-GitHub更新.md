# SOP：将 Poker_AA 代码更新到 GitHub

> 适用仓库：`https://github.com/b1polarrr/Logan-sGame.git`  
> 默认分支：`main`

---

## 一、前置条件（首次使用前检查）

| 项 | 说明 |
|---|---|
| GitHub 账号 | 已登录，对仓库有 push 权限 |
| Git 客户端 | 安装 [Git for Windows](https://git-scm.com/download/win)，**所有 git 操作用 cmd 命令行** |
| 远程仓库 | 本项目已配置 `origin` → `Logan-sGame`，一般无需重复添加 |
| 编译通过 | push 前在 IDEA 执行 **Build → Rebuild Project**，确保 proto 已生成且无报错 |

---

## 二、标准流程（每次改完代码）

> **约定**：不用 IDEA 做 Git；功能在 `frontend/` 验证，不在 `test.html` 测试。  
> 推送后在阿里云主机拉代码并重建部署（见 [开发约定.md](./开发约定.md) §1.4）。

### cmd 命令行（唯一方式）

在项目根目录 `Poker_AA` 打开 **cmd**，执行：

```cmd
REM 1. 查看状态
git status

REM 2. 添加要提交的文件（按需列出，勿提交 target/）
git add frontend/src/components/TableView.vue
git add frontend/src/composables/useGameSocket.ts

REM 3. 提交
git commit -m "fix: 前端增加准备开局"

REM 4. 推送到 GitHub
git push origin main
```

### 阿里云主机部署（拉取后测试）

```bash
cd /path/to/Poker_AA
git pull origin main
docker compose -f deploy/docker-compose.yml up -d --build
```

浏览器访问主机地址验证；前端改动必须 `--build` 才会生效。

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

cmd：`git pull origin main --rebase`，再 `git push origin main`。

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
- [ ] 通过 **cmd** Push 到 `origin/main`
- [ ] GitHub 网页上能看到最新提交
- [ ] 涉及 `frontend/` 时，已在阿里云 pull 并 **rebuild** 后再测

---

## 七、与本项目相关的远程信息

| 项 | 值 |
|---|---|
| 本地路径 | `d:\resource\javatraining\Poker_AA` |
| 远程名 | `origin` |
| 远程 URL | `https://github.com/b1polarrr/Logan-sGame.git` |
| 跟踪分支 | `main` → `origin/main` |
