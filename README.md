# 滴兔智能体工程

本仓库实现滴兔智能体单后端工程与三端前端工程：

- `backend`：Java 17 + Spring Boot 3.2 单 Maven 工程。
- `frontend/apps/h5`：用户 H5。
- `frontend/apps/admin`：管理端 Web。
- `frontend/apps/miniprogram`：用户小程序工程结构。
- `frontend/packages/*`：共享类型、API/SSE 客户端和 UI 工具。
- `deploy`：PostgreSQL/pgvector、后端、Nginx 本地部署配置。

## 本地验证

```bash
cd backend
mvn clean test
mvn package
```

```bash
cd frontend
pnpm install
pnpm typecheck
pnpm lint
pnpm build
```

## 本地启动

需要 Docker 时：

```bash
docker compose -f deploy/docker-compose.yml up --build
```

默认账号由后端首次启动初始化：

- 管理员：`admin` / `Admin123!`
- 演示用户：`demo` / `Demo123!`

核心环境变量：

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `DITU_AUTH_TOKEN_SECRET`
- `DITU_DEFAULT_MODEL_BASE_URL`
- `DITU_DEFAULT_MODEL_NAME`
- `DITU_DEFAULT_MODEL_AUTH_TYPE`
- `DITU_DEFAULT_MODEL_API_KEY`
- `DITU_EMBEDDING_MODEL`
- `DITU_FILE_STORAGE_ROOT`
