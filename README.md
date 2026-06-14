# Personal Space Java Backend

`personal-space` 的 **Spring Boot / Java 17 后端实现**。

这份仓库的目标不是简单“用 Java 重写一遍接口”，而是把原来偏一体化的个人空间项目，逐步迁到一个更清晰、更适合继续维护的后端结构里，同时尽量保持和现有前端 / 现有 JS 版接口兼容。

## 当前定位

这不是一个独立前端项目，而是：

- **给现有 personal-space 前端提供 API 的 Java 后端**
- **兼容现有 SQLite 数据结构与前端字段约定**
- **为后续正式迁移、继续扩展、补测试做准备**

如果你想看完整的一体化 Node.js 原版，可参考：

- `https://github.com/NaBr406/personal-space`

## 已实现能力

### 基础能力

- Spring Boot 3 + Java 17
- SQLite 持久化
- 启动时自动执行 schema 初始化
- 兼容旧库时自动补部分缺失列
- 默认超管自动初始化（仅首次生效）
- Actuator 健康检查
- 100MB multipart 上传限制

### 业务能力

- 注册 / 登录 / 登出 / 当前用户信息
- Bearer Token + `sessions` 表登录态
- 简单数学验证码
- 登录 / 注册基础限流
- 动态发布、详情、浏览量、删除
- 点赞、评论、回复、通知
- 多图上传与缩略图生成
- 文章 / 博客 / 轻量内容页接口
- 公告发布 / 删除 / 置顶
- 用户资料修改、头像上传、改密
- 用户列表、角色管理、删用户
- 每日邀请码、刷新邀请码
- 超管生成密码重置校验码
- 访客记录

## API 覆盖情况

当前已经覆盖这些接口：

- `GET /api/health`
- `GET /api/captcha`
- `POST /api/register`
- `POST /api/login`
- `POST /api/logout`
- `GET /api/me`
- `PUT /api/me`
- `POST /api/change-password`
- `POST /api/change-password-direct`
- `GET /api/users`
- `PUT /api/users/{id}/role`
- `DELETE /api/users/{id}`
- `GET /api/invite-code`
- `POST /api/invite-code/refresh`
- `POST /api/users/{id}/reset-code`
- `GET /api/users/{id}/reset-code`
- `POST /api/reset-password`
- `POST /api/visit`
- `GET /api/visitors`
- `GET /api/posts`
- `GET /api/posts/{id}`
- `POST /api/posts/{id}/view`
- `POST /api/posts`（JSON / multipart）
- `DELETE /api/posts/{id}`
- `POST /api/posts/{id}/like`
- `GET /api/posts/{id}/comments`
- `POST /api/posts/{id}/comments`
- `DELETE /api/comments/{id}`
- `POST /api/upload-image`
- `GET /api/notifications`
- `GET /api/notifications/unread-count`
- `POST /api/notifications/read-all`
- `POST /api/notifications/{id}/read`
- `GET /api/articles`
- `GET /api/articles/{id}`
- `POST /api/articles/{id}/view`
- `POST /api/articles`
- `PUT /api/articles/{id}`
- `DELETE /api/articles/{id}`
- `GET /api/announcements`
- `GET /api/announcements/{id}`
- `POST /api/announcements`
- `DELETE /api/announcements/{id}`
- `PATCH /api/announcements/{id}/pin`

## 和现有前端的兼容策略

为了尽量少改现有前端，这个仓库在几个地方特意保持和 JS 版一致：

- 响应字段优先沿用前端当前在用的命名，例如：
  `created_at`、`author_name`、`author_avatar`、`like_count`、`comment_count`、`cover_image`、`parent_id`
- 分页接口保留 JS 版常见返回结构：
  `{ page, limit, total, pages }`
- 编辑器上传接口保留前端需要的返回契约
- 启动时会尝试对旧版 SQLite 结构做兼容补齐，降低切换成本

更细的兼容说明见：[`COMPATIBILITY.md`](./COMPATIBILITY.md)

## 项目结构

```text
src/main/java/cn/nabr/personalspace
├── config        # 配置、数据源、启动初始化、兼容迁移
├── controller    # API 入口
├── dto           # 请求 DTO
├── exception     # 业务异常
├── model         # 返回视图模型
├── repository    # 数据访问
├── security      # 登录态解析
├── service       # 业务逻辑
└── util          # 邀请码、token 等工具
```

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/NaBr406/personal-space-java-backend.git
cd personal-space-java-backend
```

### 2. 运行测试

```bash
./mvnw test
```

### 3. 本地启动（沙盒）

```bash
bash scripts/run-sandbox.sh
```

默认端口：`3001`

健康检查：

```bash
curl http://127.0.0.1:3001/api/health
curl http://127.0.0.1:3001/actuator/health
```

### 4. 打包

```bash
./mvnw -q -DskipTests package
```

### 5. 打包后运行

```bash
bash scripts/run-prod.sh
```

### 6. 原始开发命令

```bash
./mvnw spring-boot:run
```

## 本地数据默认位置

应用默认值：

```text
数据库：./data/personal-space-java-sandbox.db
上传：./data/uploads/
```

如果直接使用仓库脚本：

- `scripts/run-sandbox.sh` 默认把数据放到 `./data/sandbox/`
- `scripts/run-prod.sh` 默认把数据放到 `./data/prod/`

## 环境变量

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `PORT` | `3001` | 后端监听端口 |
| `APP_ENV` | `sandbox` | 环境名 |
| `APP_DB_PATH` | `./data/personal-space-java-sandbox.db` | SQLite 路径 |
| `APP_DATA_DIR` | `./data` | 数据目录 |
| `APP_UPLOAD_DIR` | `./data/uploads` | 上传目录 |
| `ADMIN_PASSWORD` | `admin123` | 默认超管密码，仅首次自动创建时生效 |
| `APP_SUPER_ADMIN_USERNAME` | `NaBr406` | 默认超管用户名 |
| `APP_SUPER_ADMIN_NICKNAME` | `NaBr406` | 默认超管昵称 |
| `JAVA_OPTS` | 空 | 传给 `java` 的额外参数 |
| `JAR_PATH` | 自动识别 | 指定运行的 jar |

线上部署可直接从示例环境文件开始：

```bash
cp deploy/backend.env.example .env.production
```

## 本地联调前端

仓库附带了一个简单前端代理：

```bash
python3 tools/frontend_proxy.py
```

默认行为：

- `/api/*` 和 `/uploads/*` 转发到 `http://127.0.0.1:3001`
- 其它静态资源从现有前端目录读取

默认访问地址：

```text
http://127.0.0.1:8081
```

如果你想改代理端口或后端地址：

```bash
python3 tools/frontend_proxy.py --port 8082 --backend http://127.0.0.1:3001
```

## 上传说明

- 动态支持 `multipart/form-data`，字段 `images`，最多 9 张
- 用户头像通过 `PUT /api/me` + `avatar` 上传
- 文章封面通过 `POST/PUT /api/articles` + `cover` 上传
- 编辑器图片上传走 `POST /api/upload-image`
- 上传后的文件默认保存在 `./data/uploads/`
- 动态发布会额外生成缩略图
- 删除帖子 / 用户 / 文章时会做对应文件清理

## 部署

仓库已经提供了这些部署辅助内容：

- `deploy/backend.env.example`：环境变量示例
- `scripts/run-prod.sh`：生产启动脚本
- `scripts/run-sandbox.sh`：沙盒启动脚本
- `DEPLOY.md`：更完整的服务器部署说明

推荐配法：

- Java 后端仅监听本机端口（如 `127.0.0.1:3001`）
- Nginx 对外暴露 `80/443`
- `/api/` 和 `/uploads/` 反代到 Java 后端
- 静态页面继续由现有前端 / Nginx 提供

## 当前边界

这个仓库当前重点是 **API 与现有前端兼容**，不是完整替代所有静态页面托管逻辑。

也就是说：

- **后端接口**、**上传处理**、**SQLite 兼容** 是它的核心
- 静态 HTML 页面和现有前端结构，仍然更适合配合 Node 版前端或 Nginx 静态目录来使用

## 后续可以继续做的事

- 补自动化测试和接口回归测试
- 继续收紧与前端之间的兼容细节
- 进一步抽离 service / repository 边界
- 增加更清晰的部署与迁移脚本
- 如果后续彻底切前后端分层，再把静态资源托管方案单独整理

## 相关文档

- [DEPLOY.md](./DEPLOY.md)
- [COMPATIBILITY.md](./COMPATIBILITY.md)
- [deploy/backend.env.example](./deploy/backend.env.example)

## License

MIT
