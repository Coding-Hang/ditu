# WSL 数据库一命令启动

目录：`/data/install/db`

启动：

```bash
cd /data/install/db
./start.sh
```

非交互启动：

```bash
cd /data/install/db
SUDO_PASSWORD='your-wsl-sudo-password' ./start.sh
```

停止：

```bash
cd /data/install/db
./stop.sh
```

状态和日志：

```bash
cd /data/install/db
./status.sh
```

默认连接：

- Host：`localhost`
- Port：`5432`
- Database：`ditu`
- User：`ditu`
- Password：`ditu`
- JDBC：`jdbc:postgresql://localhost:5432/ditu`

首次启动会创建 pgvector 扩展；业务表由后端 Flyway 迁移创建。

默认镜像使用 `docker.m.daocloud.io/pgvector/pgvector:pg15`。如网络可直连 Docker Hub，可在 `.env` 中改为：

```bash
PGVECTOR_IMAGE=pgvector/pgvector:pg15
```
