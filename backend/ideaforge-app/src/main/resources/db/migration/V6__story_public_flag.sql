-- V6: 故事公开标记 + 索引
ALTER TABLE story ADD COLUMN is_public BOOLEAN DEFAULT FALSE;
CREATE INDEX idx_story_public ON story(is_public, created_at DESC) WHERE deleted_at IS NULL AND status = 'completed';
