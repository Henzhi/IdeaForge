-- V4: 将 TIMESTAMPTZ 改为 TIMESTAMP(不带时区)。
-- 原因:MyBatis 默认 LocalDateTimeTypeHandler 不支持 TIMESTAMPTZ,
-- 会导致 "Cannot convert the column of type TIMESTAMPTZ to requested type java.time.LocalDateTime"。
-- 改为 TIMESTAMP 后,PG 驱动可直接 getLocalDateTime,无需自定义 TypeHandler。
-- 应用层时区统一由 JVM(UTC)处理,不影响业务。

ALTER TABLE users ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'UTC';
ALTER TABLE users ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE user_auth ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'UTC';

ALTER TABLE idea ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'UTC';
ALTER TABLE idea ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at AT TIME ZONE 'UTC';
ALTER TABLE idea ALTER COLUMN deleted_at TYPE TIMESTAMP USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE story ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'UTC';
ALTER TABLE story ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at AT TIME ZONE 'UTC';
ALTER TABLE story ALTER COLUMN deleted_at TYPE TIMESTAMP USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE story_version ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'UTC';

ALTER TABLE model_config ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'UTC';
ALTER TABLE model_config ALTER COLUMN updated_at TYPE TIMESTAMP USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE prompt_template ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'UTC';

ALTER TABLE generation_task ALTER COLUMN started_at TYPE TIMESTAMP USING started_at AT TIME ZONE 'UTC';
ALTER TABLE generation_task ALTER COLUMN completed_at TYPE TIMESTAMP USING completed_at AT TIME ZONE 'UTC';
ALTER TABLE generation_task ALTER COLUMN created_at TYPE TIMESTAMP USING created_at AT TIME ZONE 'UTC';

ALTER TABLE sync_log ALTER COLUMN synced_at TYPE TIMESTAMP USING synced_at AT TIME ZONE 'UTC';

ALTER TABLE idea_history ALTER COLUMN snapshot_at TYPE TIMESTAMP USING snapshot_at AT TIME ZONE 'UTC';
