# Personal Space Java Sandbox Backend

这是给 `personal-space` 准备的一份 **Java 沙盒后端**。

目标不是一下子把原来的 Node.js 版本全替掉，而是先搭一版：

- 结构更清楚
- 更适合大哥以后自己看源码
- 保持和现有前端/API 思路尽量接近

## 这一版已经有的

- Spring Boot 3 + Java 17
- SQLite 数据库
- 启动时自动建表
- 自动创建默认超管 `NaBr406`
- 自动生成当天邀请码
- Bearer Token + `sessions` 表登录态
- 简单数学验证码
- 已实现接口：
  - `GET /api/health`
  - `GET /api/captcha`
  - `POST /api/register`
  - `POST /api/login`
  - `POST /api/logout`
  - `GET /api/me`
  - `GET /api/posts`
  - `GET /api/posts/{id}`
  - `POST /api/posts`
  - `DELETE /api/posts/{id}`
  - `POST /api/posts/{id}/view`
  - `POST /api/posts/{id}/like`
  - `GET /api/posts/{id}/comments`
  - `POST /api/posts/{id}/comments`
  - `DELETE /api/comments/{id}`
  - `GET /api/notifications`
  - `GET /api/notifications/unread-count`
  - `POST /api/notifications/read-all`
  - `POST /api/notifications/{id}/read`
  - `PUT /api/me`
  - `POST /api/change-password`
  - `POST /api/change-password-direct`
  - `POST /api/upload-image`
  - `GET /api/articles`
  - `GET /api/articles/{id}`
  - `POST /api/articles/{id}/view`
  - `POST /api/articles`
  - `PUT /api/articles/{id}`
  - `DELETE /api/articles/{id}`
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
  - `GET /api/announcements`
  - `GET /api/announcements/{id}`
  - `POST /api/announcements`
  - `DELETE /api/announcements/{id}`
  - `PATCH /api/announcements/{id}/pin`

## 当前还在收尾的

这版已经能本地跑，也补了基础部署说明；现在主要还在收尾这些：

- 前端联调与接口细节打磨
- 上线前再做一轮实际站点验证

## 和 JS 版的对应关系

这版是按原来 JS 项目的思路拆的：

- `controller`：对应原来的路由入口
- `service`：对应业务逻辑
- `repository`：对应数据库查询
- `security`：登录态解析
- `config`：配置和启动初始化
- `util`：邀请码、token hash 之类的小工具

### 当前兼容性说明

为了尽量少改现有前端，Java 版在关键返回字段上继续沿用 JS 版习惯：

- 动态 / 评论 / 通知 / 公告 / 文章 / 访客 等响应，优先保持 `snake_case`
- 例如：`created_at`、`author_name`、`author_avatar`、`like_count`、`comment_count`、`cover_image`、`parent_id`
- 登录、注册、`/api/me` 这类用户信息接口，继续保留前端已经在用的普通字段（如 `nickname`、`avatar`、`role`）

这样做的目的是：**尽量让现有 JS 前端少改就能直接接 Java 后端**。

## 目录结构

```text
src/main/java/cn/nabr/personalspace
├── config
├── controller
├── dto
├── exception
├── model
├── repository
├── security
├── service
└── util
```

## 运行

### 启动测试

```bash
./mvnw test
```

### 本地沙盒启动

```bash
bash scripts/run-sandbox.sh
```

### 打包

```bash
./mvnw -q -DskipTests package
```

### 打包后运行

```bash
bash scripts/run-prod.sh
```

### 原始开发命令

```bash
./mvnw spring-boot:run
```

默认端口：`3001`

数据库默认放在：

```text
./data/personal-space-java-sandbox.db
```

如果你直接用仓库脚本：

- `scripts/run-sandbox.sh` 默认写到 `./data/sandbox/`
- `scripts/run-prod.sh` 默认写到 `./data/prod/`

## 可配环境变量

- `PORT`：端口，默认 `3001`
- `APP_ENV`：环境名，默认 `sandbox`
- `APP_DB_PATH`：SQLite 文件路径
- `APP_DATA_DIR`：数据目录
- `APP_UPLOAD_DIR`：上传目录
- `ADMIN_PASSWORD`：默认超管密码，仅**首次自动创建超管**时生效
- `APP_SUPER_ADMIN_USERNAME`：默认超管用户名
- `APP_SUPER_ADMIN_NICKNAME`：默认超管昵称

更完整的服务器部署、`systemd`、`Nginx`、前端配对说明见：`DEPLOY.md`

## 当前上传实现说明

- 发帖支持 `multipart/form-data`，字段 `images` 最多 9 张
- 头像上传已经支持 `PUT /api/me` + `avatar`
- 上传后的文件会保存到 `./data/uploads/`
- 访问路径是 `/uploads/文件名`
- 发帖图片会额外生成真实缩略图（最长边不超过 `480px`，`jpg`）
- 帖子里：
  - `image` / `thumbnail`：第一张图及其缩略图
  - `images` / `thumbnails`：完整图片数组和缩略图数组（JSON 字符串）
- 删除清理规则：
  - 删除帖子会清理该帖的原图和缩略图（含数组字段）
  - 更新头像、更新文章封面时，会在更新成功后删除旧文件
  - 删除用户时，会清理该用户头像、其所有帖子图片/缩略图、其所有文章封面

## 本地联调前端

仓库里带了一个简易代理脚本：

```bash
python3 tools/frontend_proxy.py
```

默认会做两件事：

- 把 `/api/*` 和 `/uploads/*` 转发到 Java 后端 `http://127.0.0.1:3001`
- 其它页面资源直接从现有前端目录 `personal-space-java-sandbox/public` 读取

启动后访问：

```text
http://127.0.0.1:8081
```

如果你想换端口或后端地址，也可以：

```bash
python3 tools/frontend_proxy.py --port 8082 --backend http://127.0.0.1:3001
```

## 部署速览

- 后端打包后可直接用：`bash scripts/run-prod.sh`
- 生产环境变量样例：`deploy/backend.env.example`
- 服务器部署步骤：`DEPLOY.md`
- 如果前端还是沿用现有代理脚本，继续用：`tools/frontend_proxy.py`
- 如果前端已经是静态文件，直接让 `Nginx` 代理 `/api/` 和 `/uploads/` 到 Java 后端即可

## 适合怎么继续往下做

我建议后面按这个顺序补：

1. 接前端联调（重点验证多图/缩略图字段）
2. 接沙盒前端实测
3. 再考虑正式迁移

这样你以后看源码时，会比直接啃原来那坨 JS 更有参与感。
