-- V1: 扩展安装。pgvector 必须由专用镜像 pgvector/pgvector:pg16 提供。
-- 普通 postgres 镜像执行此语句会失败,详见 Docker部署设计.md。
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
