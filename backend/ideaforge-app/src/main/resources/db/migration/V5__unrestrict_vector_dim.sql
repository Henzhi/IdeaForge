-- V5: 移除 embedding 列的固定维度限制。
-- 不同模型输出维度不同(OpenAI 1536 / bge-large-zh-v1.5 1024 / etc),
-- 建表时写死 VECTOR(1536) 会导致其他维度模型写入失败。
-- 改为无维度限制的 vector 类型,pgvector 自动适配。

ALTER TABLE idea  ALTER COLUMN embedding TYPE vector;
ALTER TABLE story ALTER COLUMN embedding TYPE vector;
