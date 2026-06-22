-- V3: 初始化数据。系统预置分类与默认提示词模板。
INSERT INTO category (name, color, icon, sort_order) VALUES
('灵感火花', '#FF8C42', 'lightbulb', 1),
('人物角色', '#4A6A7C', 'person', 2),
('地点场景', '#5E8C5E', 'location_city', 3),
('情节片段', '#C44569', 'auto_stories', 4),
('对话台词', '#9B59B6', 'forum', 5),
('其他', '#AAAAAA', 'more_horiz', 99)
ON CONFLICT DO NOTHING;

INSERT INTO prompt_template (user_id, name, content, is_system) VALUES
(NULL, '标准故事生成', E'你是一个创意作家，根据以下思想碎片创作一个连贯的{style}故事，视角为{tone}，长度约{length}字。\n\n思想碎片：\n{ideas}', TRUE)
ON CONFLICT DO NOTHING;
