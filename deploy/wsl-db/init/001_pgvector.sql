-- DEV-001: 数据库容器初始化 pgvector 扩展，后端 Flyway 迁移会继续创建业务表和字段注释。
CREATE EXTENSION IF NOT EXISTS vector;
