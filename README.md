# math-library

一个面向读者、馆员和管理员的智能图书管理系统。项目将传统图书馆流通业务与受控的 AgentAI 助手结合，覆盖图书检索、借阅流转、预约通知、馆藏管理、合规电子阅读和审计运维。

## 项目解决什么问题

传统图书管理系统通常只能完成固定按钮操作，读者需要在多个页面之间查找图书、确认库存、借阅或预约；管理员则需要分别维护馆藏、用户和流通记录。本项目提供统一的前后端系统和自然语言入口：

- 读者可以从检索、借阅、预约、阅读和通知页面完成完整的阅读流程；
- 馆员和管理员可以维护馆藏、查看借阅统计和审计日志；
- AgentAI 可以查询真实业务数据、理解阅读或借阅意图，并把有限的写操作转换为需要用户明确确认的短期操作草案；
- 电子阅读只开放经过服务端权属和来源白名单核验的资源，避免任意外部链接或未授权正文进入系统。

## 主要功能

### 读者端

- JWT 登录和角色权限控制；
- 图书列表、关键词搜索、分页排序、详情和相似推荐；
- 借阅、归还、续借、预约、愿望单；
- 个人借阅记录、阅读进度和站内通知；
- 合规电子书章节阅读，支持恢复上次阅读进度；
- AgentAI 图书助手：馆藏问答、推荐、借阅/预约意图识别和阅读导航。

### 馆员与管理员端

- 新增、编辑、停借、恢复和删除馆藏；
- 馆藏候选方案和批量管理提案；
- 借阅记录、逾期提醒、分类统计和操作日志；
- 用户创建、角色调整、账号启停和密码重置（管理员）；
- 电子资源下架和版权闸门管理。

### Agent 安全边界

Agent 的查询工具是只读的。预约、续借、愿望单和馆藏变更不会由模型直接执行，而是生成绑定当前 JWT 用户、默认 5 分钟有效的操作草案；用户确认后，服务端会重新校验身份、角色、库存和业务引用。删除馆藏仅允许管理员，且存在借阅历史、预约历史或其他引用时会被拒绝。

## 技术栈

- 后端：Java 21、Spring Boot 3.2、Spring Security、Spring Data JPA、Flyway、PostgreSQL；
- 前端：Vue 3、Vue Router、Pinia、Element Plus、Axios；
- AI：可选 DeepSeek API，密钥只从环境变量读取；
- 部署：Docker Compose、PostgreSQL 16、Nginx；
- 接口：REST JSON、SSE Agent 流式接口、OpenAPI/Swagger；
- 测试：JUnit、Spring Boot Test、前端生产构建和 GitHub Actions CI。

## 安装方法

### 前置条件

- Docker Desktop 及 Docker Compose；
- 或手动开发时安装 Java 21、Maven、Node.js 18+ 和 PostgreSQL；
- 如启用 Agent，需要可用的 DeepSeek API Key；
- 如启用电子阅读，后端运行环境需要能够通过 HTTPS 访问 `zh.wikisource.org`。

### Docker Compose（推荐）

在项目根目录执行：

```powershell
Copy-Item .env.example .env
```

编辑 `.env`，至少设置强随机的 `DB_PASSWORD` 和 `JWT_SECRET`。可以用下面的 PowerShell 命令生成 JWT 密钥：

```powershell
$jwtBytes = New-Object byte[] 48
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($jwtBytes)
$rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
```

首次部署时，可临时设置管理员初始化变量：

```text
APP_BOOTSTRAP_ADMIN_ENABLED=true
APP_BOOTSTRAP_ADMIN_USER=your-admin-id
APP_BOOTSTRAP_ADMIN_PASSWORD=your-strong-password
APP_BOOTSTRAP_ADMIN_NAME=Library Administrator
```

启动服务：

```powershell
docker compose up --build -d
docker compose ps
```

管理员创建成功后，将 `APP_BOOTSTRAP_ADMIN_ENABLED` 改为 `false` 并重新创建后端容器：

```powershell
docker compose up -d backend
```

停止服务：

```powershell
docker compose down
```

生产环境不要使用 `docker compose down -v`，除非确认可以删除 PostgreSQL 数据卷。

### 手动开发启动

手动启动需要准备 PostgreSQL，并为后端提供 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 和 `JWT_SECRET`。后端默认端口为 `8091`，前端开发服务器默认端口为 `8082`。

```powershell
# 终端 1：后端
cd <project-root>\backend
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DB_URL = "jdbc:postgresql://localhost:5432/library"
$env:DB_USERNAME = "library"
$env:DB_PASSWORD = "your-database-password"
$env:JWT_SECRET = "your-long-random-jwt-secret"
$env:APP_DEMO_DATA_ENABLED = "false"
mvn spring-boot:run

# 终端 2：前端
cd <project-root>\frontend
npm install
npm run dev
```

前端通过开发代理访问后端 `/api` 路径。生产构建命令为：

```powershell
npm run build
```

## 使用方法

1. 打开 `http://localhost:8082/login`。
2. 使用部署时创建的账号登录。仓库不提供默认账号或公开密码。
3. 读者进入首页后可以搜索图书、查看详情、借阅、预约、管理愿望单和打开合规阅读资源。
4. 在 `/qa` 进入 AgentAI 图书助手，可以直接询问馆藏、推荐或阅读导航。
5. 当 Agent 生成预约、续借或馆藏操作草案时，先核对候选图书和参数，再点击确认；不需要执行时可以取消。
6. `ADMIN` 或 `LIBRARIAN` 进入 `/admin` 管理馆藏；用户管理和彻底删除书目仅限 `ADMIN`。

常用地址：

| 地址 | 用途 |
|---|---|
| `http://localhost:8082` | 前端应用 |
| `http://localhost:8091` | 后端服务 |
| `http://localhost:8091/actuator/health` | 健康检查 |
| `http://localhost:8091/swagger-ui.html` | Swagger UI，需管理员访问 |
| `http://localhost:8091/v3/api-docs` | OpenAPI JSON，需管理员访问 |

## 输入输出示例

### 登录

请求：

```http
POST /api/auth/login
Content-Type: application/json

{
  "userId": "your-user-id",
  "password": "your-password"
}
```

成功响应中的 `data.token` 用于后续请求：

```json
{
  "success": true,
  "code": "OK",
  "message": "登录成功",
  "data": {
    "token": "<access-token>",
    "userId": "your-user-id",
    "name": "Reader",
    "role": "USER"
  }
}
```

### 图书搜索

请求：

```http
GET /api/books/search?keyword=科幻
Authorization: Bearer <access-token>
```

响应：

```json
{
  "success": true,
  "code": "OK",
  "message": "查询成功",
  "data": [
    {
      "isbn": "<isbn>",
      "title": "示例图书",
      "author": "示例作者",
      "totalCount": 3,
      "borrowedCount": 1,
      "availableCount": 2,
      "borrowable": true
    }
  ]
}
```

### Agent 问答

请求：

```http
POST /api/qa/agent/run
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "question": "推荐一本现在可借的科幻小说，并告诉我位置",
  "sessionId": "reading-session-001"
}
```

响应可能包含查询结果和工具调用摘要：

```json
{
  "success": true,
  "code": "OK",
  "data": {
    "answer": "目前有 2 本可借的科幻小说，推荐《示例图书》，位置为 A-03。",
    "sessionId": "reading-session-001",
    "toolsUsed": ["search_books"],
    "iterations": 2,
    "pendingAction": null,
    "navigation": null
  }
}
```

当用户请求“预约《某书》”或“续借《某书》”时，`pendingAction` 会返回待确认草案；用户确认后才会执行实际业务操作。阅读导航只返回固定的 `OPEN_EBOOK` 结构，不接受模型生成的任意 URL。

### 合规电子阅读

请求：

```http
GET /api/books/<isbn>/ebook/chapters/1
Authorization: Bearer <access-token>
```

响应只包含经过后端清洗的章节正文、来源、许可证和署名信息。未通过权属或发布闸门的资源会返回错误，不会回退到未知镜像。

## 配置与安全说明

- 不要提交 `.env`、真实 API Key、JWT 密钥、数据库文件、运行日志或构建目录；
- `APP_DEMO_DATA_ENABLED` 默认关闭，项目不提供公开演示账号；
- `APP_BOOTSTRAP_ADMIN_ENABLED` 仅用于首次初始化管理员，完成后必须关闭；
- Agent 的写操作必须经过用户确认和服务端二次校验；
- 生产环境请使用 PostgreSQL、HTTPS、反向代理和独立密钥管理服务；
- 版权清单和电子资源使用边界见 `docs/电子资源版权清单.md`；
- 完整接口约定见 `docs/接口文档.md`，项目部署说明见 `docs/项目说明书.md`。

## 开发验证

```powershell
cd <project-root>\backend
mvn test

cd <project-root>\frontend
npm run build
```

GitHub Actions 会在推送和 Pull Request 时执行后端验证与前端生产构建。

