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
  - `GET /api/announcements`
  - `GET /api/announcements/{id}`
  - `POST /api/announcements`
  - `DELETE /api/announcements/{id}`
  - `PATCH /api/announcements/{id}/pin`

## 还没迁过去的

这版已经进入第三期，下面这些还没做：

- 图片上传（现在还是纯文字发帖）
- 用户头像上传
- 用户管理
- 文章 / 博客 / 杂谈
- 密码重置
- 访客记录
- 多图上传与缩略图

## 和 JS 版的对应关系

这版是按原来 JS 项目的思路拆的：

- `controller`：对应原来的路由入口
- `service`：对应业务逻辑
- `repository`：对应数据库查询
- `security`：登录态解析
- `config`：配置和启动初始化
- `util`：邀请码、token hash 之类的小工具

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

### 打包

```bash
./mvnw -q -DskipTests package
```

### 运行

```bash
./mvnw spring-boot:run
```

默认端口：`3001`

数据库默认放在：

```text
./data/personal-space-java-sandbox.db
```

## 可配环境变量

- `PORT`：端口，默认 `3001`
- `APP_DB_PATH`：SQLite 文件路径
- `APP_DATA_DIR`：数据目录
- `APP_ENV`：环境名，默认 `sandbox`
- `ADMIN_PASSWORD`：默认超管密码

## 适合怎么继续往下做

我建议后面按这个顺序补：

1. 上传图片 / 头像上传
2. 文章系统
3. 用户管理
4. 密码重置
5. 访客记录
6. 多图与缩略图

这样你以后看源码时，会比直接啃原来那坨 JS 更有参与感。
