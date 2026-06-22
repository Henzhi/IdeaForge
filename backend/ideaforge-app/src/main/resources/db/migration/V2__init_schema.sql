-- V2: 核心表结构。对应 IdeaForge数据库设计.md。
-- 关键修正:
--   1. 表名 users(避开 PG 保留字 user)
--   2. idea.client_uuid 用 partial unique index(仅未软删行),避免软删后同 UUID 重建冲突
--   3. embedding 维度 1536(可按模型调整,见数据库设计文档向量迁移方案)

-- ===== 用户与认证 =====
CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    username      VARCHAR(50)  UNIQUE NOT NULL,
    nickname      VARCHAR(100),
    avatar_url    TEXT,
    email         VARCHAR(255) UNIQUE,
    phone         VARCHAR(20)  UNIQUE,
    password_hash VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    status        SMALLINT     NOT NULL DEFAULT 1
);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);

CREATE TABLE user_auth (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider     VARCHAR(20)  NOT NULL,
    provider_id  VARCHAR(255) NOT NULL,
    access_token TEXT,
    created_at   TIMESTAMPTZ  DEFAULT NOW(),
    CONSTRAINT uk_provider_pid UNIQUE (provider, provider_id)
);
CREATE INDEX idx_user_auth_user ON user_auth(user_id);

-- ===== 分类与标签 =====
CREATE TABLE category (
    id         SMALLSERIAL PRIMARY KEY,
    name       VARCHAR(30) NOT NULL,
    color      VARCHAR(7)  DEFAULT '#FF8C42',
    icon       VARCHAR(50),
    sort_order INT         DEFAULT 0,
    created_by BIGINT      REFERENCES users(id)
);

CREATE TABLE tag (
    id         BIGSERIAL   PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    color      VARCHAR(7)  DEFAULT '#AAAAAA',
    user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uk_tag_name_user UNIQUE (name, user_id)
);

-- ===== 想法 =====
CREATE TABLE idea (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content     TEXT         NOT NULL,
    category_id SMALLINT     REFERENCES category(id),
    embedding   VECTOR(1536),
    is_archived BOOLEAN      DEFAULT FALSE,
    is_pinned   BOOLEAN      DEFAULT FALSE,
    client_uuid VARCHAR(36),
    created_at  TIMESTAMPTZ  DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);
CREATE INDEX idx_idea_user_created  ON idea(user_id, created_at DESC);
CREATE INDEX idx_idea_user_category ON idea(user_id, category_id);
CREATE INDEX idx_idea_embedding     ON idea USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
-- partial unique index: 仅未软删行生效,解决软删后同 UUID 重建冲突
CREATE UNIQUE INDEX idx_idea_user_uuid ON idea(user_id, client_uuid) WHERE deleted_at IS NULL;

CREATE TABLE idea_tag (
    idea_id BIGINT NOT NULL REFERENCES idea(id) ON DELETE CASCADE,
    tag_id  BIGINT NOT NULL REFERENCES tag(id)  ON DELETE CASCADE,
    PRIMARY KEY (idea_id, tag_id)
);

-- ===== 故事 =====
CREATE TABLE story (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    content         TEXT,
    summary         TEXT,
    word_count      INT          DEFAULT 0,
    status          VARCHAR(20)  DEFAULT 'draft',
    style           VARCHAR(30),
    tone            VARCHAR(30),
    length_preset   VARCHAR(20),
    cover_image_url TEXT,
    embedding       VECTOR(1536),
    view_count      INT          DEFAULT 0,
    created_at      TIMESTAMPTZ  DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);
CREATE INDEX idx_story_user_status ON story(user_id, status);
CREATE INDEX idx_story_embedding   ON story USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

CREATE TABLE story_version (
    id                 BIGSERIAL    PRIMARY KEY,
    story_id           BIGINT       NOT NULL REFERENCES story(id) ON DELETE CASCADE,
    version_number     INT          NOT NULL,
    content            TEXT         NOT NULL,
    word_count         INT,
    change_summary     VARCHAR(200),
    generation_task_id BIGINT,
    created_at         TIMESTAMPTZ  DEFAULT NOW(),
    CONSTRAINT uk_story_version UNIQUE (story_id, version_number)
);

CREATE TABLE story_idea (
    story_id   BIGINT NOT NULL REFERENCES story(id) ON DELETE CASCADE,
    idea_id    BIGINT NOT NULL REFERENCES idea(id)  ON DELETE CASCADE,
    sort_order INT    DEFAULT 0,
    PRIMARY KEY (story_id, idea_id)
);

CREATE TABLE story_tag (
    story_id BIGINT NOT NULL REFERENCES story(id) ON DELETE CASCADE,
    tag_id   BIGINT NOT NULL REFERENCES tag(id)   ON DELETE CASCADE,
    PRIMARY KEY (story_id, tag_id)
);

-- ===== AI 模型配置 =====
CREATE TABLE model_config (
    id                BIGSERIAL   PRIMARY KEY,
    user_id           BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider          VARCHAR(30) NOT NULL,
    model_name        VARCHAR(50) NOT NULL,
    api_key_encrypted TEXT        NOT NULL,
    embedding_dim     INT         DEFAULT 1536,
    base_url          TEXT,
    temperature       FLOAT       DEFAULT 0.8,
    top_p             FLOAT       DEFAULT 1.0,
    max_tokens        INT         DEFAULT 2000,
    is_default        BOOLEAN     DEFAULT FALSE,
    created_at        TIMESTAMPTZ DEFAULT NOW(),
    updated_at        TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uk_model_user_provider UNIQUE (user_id, provider, model_name)
);

CREATE TABLE prompt_template (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       REFERENCES users(id),
    name        VARCHAR(100) NOT NULL,
    content     TEXT         NOT NULL,
    description TEXT,
    is_system   BOOLEAN      DEFAULT FALSE,
    created_at  TIMESTAMPTZ  DEFAULT NOW()
);

-- ===== 生成任务 =====
CREATE TABLE generation_task (
    id                 BIGSERIAL    PRIMARY KEY,
    user_id            BIGINT       NOT NULL REFERENCES users(id),
    story_id           BIGINT       REFERENCES story(id),
    model_config_id    BIGINT       REFERENCES model_config(id),
    prompt_template_id BIGINT       REFERENCES prompt_template(id),
    status             VARCHAR(20)  DEFAULT 'queued',
    input_ideas        JSONB        NOT NULL,
    parameters         JSONB,
    prompt_text        TEXT,
    error_message      TEXT,
    tokens_used        INT,
    cost               DECIMAL(10,6),
    retry_count        INT          DEFAULT 0,
    started_at         TIMESTAMPTZ,
    completed_at       TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  DEFAULT NOW()
);
CREATE INDEX idx_task_user_status   ON generation_task(user_id, status);
CREATE INDEX idx_task_status_created ON generation_task(status, created_at);

-- ===== 辅助表 =====
CREATE TABLE sync_log (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     BIGINT,
    entity_type VARCHAR(20),
    entity_id   BIGINT,
    action      VARCHAR(10),
    synced_at   TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE idea_history (
    id          BIGSERIAL   PRIMARY KEY,
    idea_id     BIGINT      REFERENCES idea(id) ON DELETE CASCADE,
    user_id     BIGINT,
    content     TEXT,
    client_uuid VARCHAR(36),
    snapshot_at TIMESTAMPTZ DEFAULT NOW()
);
