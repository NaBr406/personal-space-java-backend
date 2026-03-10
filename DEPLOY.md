# 部署说明

这份说明尽量只做两件事：

- 让后端先稳定跑起来
- 让你能很快和现有前端接上

如果你只想先试跑，最短路径是：

```bash
./mvnw -q -DskipTests package
cp deploy/backend.env.example .env.production
vim .env.production
set -a
source ./.env.production
set +a
bash scripts/run-prod.sh
```

跑起来后先检查：

```bash
curl http://127.0.0.1:3001/api/health
curl http://127.0.0.1:3001/actuator/health
```

## 1. 服务器上怎么跑

### 方案 A：先直接跑 jar

适合第一次上服务器、先确认服务没问题。

```bash
git clone <your-repo-url>
cd personal-space-java-backend
./mvnw -q -DskipTests package

cp deploy/backend.env.example .env.production
vim .env.production

set -a
source ./.env.production
set +a

bash scripts/run-prod.sh
```

`scripts/run-prod.sh` 会帮你做这几件事：

- 自动找 `target/` 里的可运行 jar
- 自动创建数据库目录和上传目录
- 读取你已经导出的环境变量
- 最后执行 `java -jar ...`

### 推荐目录

如果是正式服务器，比较推荐这样放：

```text
/opt/personal-space-java-backend         # 仓库 / jar / 脚本
/var/lib/personal-space-java-backend     # SQLite 和 uploads
```

这样升级代码和备份数据会更清楚。

## 2. 环境变量

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `PORT` | `3001` | 后端监听端口 |
| `APP_ENV` | `sandbox` | 环境名，建议线上改成 `prod` |
| `APP_DB_PATH` | `./data/personal-space-java-sandbox.db` | SQLite 文件路径 |
| `APP_DATA_DIR` | `./data` | 数据目录，启动时会自动创建 |
| `APP_UPLOAD_DIR` | `./data/uploads` | 上传文件目录 |
| `ADMIN_PASSWORD` | `admin123` | 默认超管密码，仅首次自动建超管时生效 |
| `APP_SUPER_ADMIN_USERNAME` | `NaBr406` | 默认超管用户名 |
| `APP_SUPER_ADMIN_NICKNAME` | `NaBr406` | 默认超管昵称 |

上表写的是**应用本身**的默认值。

如果你直接用仓库里的脚本：

- `scripts/run-sandbox.sh` 会默认使用 `./data/sandbox/`
- `scripts/run-prod.sh` 会默认使用 `./data/prod/`

脚本额外支持：

| 变量 | 说明 |
| --- | --- |
| `JAR_PATH` | 指定要运行的 jar；不填就自动从 `target/` 里找 |
| `JAVA_OPTS` | 传给 `java` 的额外参数，比如内存或 `server.address` |

### 一个实用提醒

`APP_DATA_DIR` **不会自动改掉** `APP_DB_PATH` 和 `APP_UPLOAD_DIR`。

也就是说，如果你准备把数据放到：

```text
/var/lib/personal-space-java-backend
```

最好把这三个一起改：

```bash
APP_DATA_DIR=/var/lib/personal-space-java-backend
APP_DB_PATH=/var/lib/personal-space-java-backend/personal-space-java.db
APP_UPLOAD_DIR=/var/lib/personal-space-java-backend/uploads
```

## 3. `systemd` 示例

先准备环境文件：

```bash
sudo cp deploy/backend.env.example /etc/personal-space-java-backend.env
sudo vim /etc/personal-space-java-backend.env
```

然后新建：

```text
/etc/systemd/system/personal-space-java-backend.service
```

内容示例：

```ini
[Unit]
Description=Personal Space Java Backend
After=network.target

[Service]
Type=simple
User=personal-space
Group=personal-space
WorkingDirectory=/opt/personal-space-java-backend
EnvironmentFile=/etc/personal-space-java-backend.env
ExecStart=/opt/personal-space-java-backend/scripts/run-prod.sh
Restart=always
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

启用方式：

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now personal-space-java-backend
sudo systemctl status personal-space-java-backend
sudo journalctl -u personal-space-java-backend -f
```

注意两点：

- `User` / `Group` 要对 `APP_DB_PATH` 和 `APP_UPLOAD_DIR` 有写权限
- 第一次上线前，记得先改 `ADMIN_PASSWORD`

## 4. `Nginx` 反向代理示例

### 方案 A：前端是静态文件

这是最常见、也最简单的方式。

```nginx
server {
    listen 80;
    server_name your-domain.com;
    client_max_body_size 100m;

    root /var/www/personal-space-frontend;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:3001;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /uploads/ {
        proxy_pass http://127.0.0.1:3001;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

这套方式适合：

- 前端已经打包成静态文件
- 你希望站点只暴露 `80/443`
- 后端只在本机 `3001` 提供服务

### 方案 B：继续搭配现有前端代理脚本

如果你还没拆掉现有前端结构，可以继续用仓库里的：

```bash
python3 tools/frontend_proxy.py \
  --host 127.0.0.1 \
  --port 8081 \
  --backend http://127.0.0.1:3001 \
  --public-dir /path/to/personal-space-frontend/public
```

这时 `Nginx` 可以直接把整个站点转给这个代理：

```nginx
server {
    listen 80;
    server_name your-domain.com;
    client_max_body_size 100m;

    location / {
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

这套方式适合：

- 你还在沿用现有 `public/` 前端
- 想先低成本验证 Java 后端
- 暂时不想改前端部署方式

## 5. 怎么和前端配

可以直接按下面二选一：

### 方式 1：静态前端 + `Nginx`

- 前端页面由 `Nginx` 直接提供
- `/api/` 和 `/uploads/` 反代到 Java 后端
- 这是更像正式环境的方式

### 方式 2：现有前端代理 + Java 后端

- 用 `tools/frontend_proxy.py` 读现有前端 `public/`
- 代理脚本把 `/api/` 和 `/uploads/` 转给 Java 后端
- 更适合联调期和迁移期

如果前端代码仍然请求相对路径 `/api/...` 和 `/uploads/...`，这两种方式都能直接接上。

## 6. 常用检查命令

```bash
curl http://127.0.0.1:3001/api/health
curl http://127.0.0.1:3001/actuator/health
ls -lah /var/lib/personal-space-java-backend
```

如果服务已经交给 `systemd`：

```bash
sudo systemctl status personal-space-java-backend
sudo journalctl -u personal-space-java-backend -n 100
```
