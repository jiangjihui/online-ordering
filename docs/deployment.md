# 部署文档

## 环境要求

| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| Java | 17+ | 后端运行环境 |
| Node.js | 18+ | 前端构建 |
| Maven | 3.8+ | 后端构建 |
| Nginx | 1.18+ | 反向代理（推荐） |

---

## 生产部署架构

```
客户端浏览器
    ↓
  Nginx (80/443)
    ├── /           → 前端静态文件 (dist/)
    ├── /api/       → 后端 API (localhost:8080)
    ├── /ws         → WebSocket (localhost:8080)
    └── /images/    → 后端静态图片 (localhost:8080)
```

---

## 1. 后端构建与部署

### 构建 JAR

```bash
cd backend
export JAVA_HOME=/path/to/jdk-17
mvn clean package -DskipTests
```

构建产物位于 `backend/target/online-ordering-0.0.1-SNAPSHOT.jar`。

### 运行

```bash
java -jar online-ordering-0.0.1-SNAPSHOT.jar
```

### 自定义配置

可通过命令行参数覆盖默认配置：

```bash
# 修改端口
java -jar online-ordering-0.0.1-SNAPSHOT.jar --server.port=9090

# 修改数据库路径
java -jar online-ordering-0.0.1-SNAPSHOT.jar --spring.datasource.url=jdbc:sqlite:/data/ordering.db
```

也可以使用外部配置文件 `application.yml`：

```bash
java -jar online-ordering-0.0.1-SNAPSHOT.jar --spring.config.additional-location=/etc/ordering/application.yml
```

### 后台运行（Linux）

```bash
# 使用 nohup
nohup java -jar online-ordering-0.0.1-SNAPSHOT.jar > ordering.log 2>&1 &

# 或使用 systemd 服务（推荐）
```

### Systemd 服务示例

创建 `/etc/systemd/system/ordering.service`：

```ini
[Unit]
Description=Online Ordering System
After=network.target

[Service]
Type=simple
User=ordering
WorkingDirectory=/opt/ordering
ExecStart=/usr/bin/java -jar /opt/ordering/online-ordering.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable ordering
sudo systemctl start ordering
```

---

## 2. 前端构建

### 构建

```bash
cd frontend
npm install
npm run build
```

构建产物位于 `frontend/dist/` 目录。

### 环境变量

前端使用 Vite 环境变量配置 API 地址：

**开发环境** `.env.development`（已配置）：
```
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_BASE_URL=http://localhost:8080/ws
VITE_IMAGE_BASE_URL=http://localhost:8080/images
```

**生产环境** `.env.production`（已配置）：
```
VITE_API_BASE_URL=/api
VITE_WS_BASE_URL=/ws
VITE_IMAGE_BASE_URL=/images
```

生产环境使用相对路径，由 Nginx 反向代理转发到后端。无需修改，直接构建即可。

---

## 3. Nginx 配置

### 基础配置

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /opt/ordering/dist;
        index index.html;
        try_files $uri $uri/ /index.html;  # Vue Router history 模式
    }

    # 后端 API
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # WebSocket
    location /ws {
        proxy_pass http://127.0.0.1:8080/ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_read_timeout 86400;  # WebSocket 长连接超时
    }

    # 菜品图片
    location /images/ {
        proxy_pass http://127.0.0.1:8080/images/;
    }

    # 上传图片大小限制
    client_max_body_size 10m;
}
```

### HTTPS 配置（推荐）

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /etc/ssl/certs/your-domain.pem;
    ssl_certificate_key /etc/ssl/private/your-domain.key;

    # ... 其余配置同上
}

server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$host$request_uri;
}
```

---

## 4. 数据库

### SQLite

默认使用 SQLite，数据库文件位于 `{工作目录}/data/ordering.db`。

- 首次启动自动创建数据库和表（Flyway 迁移）
- 种子数据自动插入（V2__seed_data.sql）
- 图片上传目录：`{工作目录}/data/images/`

### 备份

```bash
# SQLite 备份（在后端停止时执行）
cp data/ordering.db data/ordering_backup_$(date +%Y%m%d).db

# 或使用 sqlite3 命令在线备份
sqlite3 data/ordering.db ".backup data/ordering_backup.db"
```

---

## 5. 安全注意事项

### 修改默认密码

生产环境务必修改 `application.yml` 中的默认账号密码：

```yaml
app:
  security:
    users:
      - username: admin
        password: your-strong-password   # 修改为强密码
        roles: ADMIN
      - username: kitchen
        password: your-strong-password   # 修改为强密码
        roles: KITCHEN
```

> 当前密码使用明文存储（`{noop}`），适用于小型部署。如需更高安全性，建议替换为 BCrypt 加密。

### 其他建议

- 启用 HTTPS（Nginx 配置 SSL 证书）
- 限制管理后台的 IP 访问范围
- 定期备份数据库
- 修改默认端口（非 8080）
- 配置防火墙规则，仅开放 80/443 端口

---

## 6. Docker 部署（可选）

### 后端 Dockerfile

```dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/online-ordering-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose

```yaml
version: '3.8'
services:
  backend:
    build: ./backend
    ports:
      - "8080:8080"
    volumes:
      - ./data:/app/data    # 持久化数据库和图片
    restart: unless-stopped

  frontend:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./frontend/dist:/usr/share/nginx/html
      - ./nginx.conf:/etc/nginx/conf.d/default.conf
    depends_on:
      - backend
    restart: unless-stopped
```

```bash
# 构建后端
cd backend && mvn clean package -DskipTests

# 构建前端
cd frontend && npm install && npm run build

# 启动
docker-compose up -d
```

---

## 7. 常见问题

### Q: 后端启动报错 `UnsupportedClassVersionError`

Spring Boot 3.2 要求 Java 17+，请检查 Java 版本：

```bash
java -version
# 应输出 17.x 或更高版本
```

如系统有多个 Java 版本，显式指定：

```bash
export JAVA_HOME=/path/to/jdk-17
```

### Q: 前端无法连接后端 API

1. 确认后端已启动并监听 8080 端口
2. 开发环境检查 `.env.development` 中的地址是否正确
3. 生产环境检查 Nginx 反向代理配置

### Q: WebSocket 连接失败

1. 确认 Nginx 配置了 WebSocket 代理（`Upgrade` 头）
2. 检查 `proxy_read_timeout` 设置，WebSocket 需要较长超时
3. 如果使用了 CDN，确认支持 WebSocket 协议

### Q: 图片上传失败

1. 确认 `data/images/` 目录存在且有写入权限
2. Nginx 配置 `client_max_body_size` 限制（默认 1MB，建议设为 10m）
3. 检查后端日志确认具体错误
