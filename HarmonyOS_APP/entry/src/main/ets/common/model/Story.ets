/**
 * 故事数据模型 — 对应后端 /stories 接口返回结构。
 */

/** 故事实体 */
export interface Story {
  id: number;
  userId: number;
  title: string;
  content: string;
  summary: string;
  wordCount: number;
  status: string;       // draft / completed
  isPublic: boolean;
  createdAt: string;
  updatedAt: string;
}

/** 生成请求体 */
export interface GenerateBody {
  ideaIds: number[];
  style: string;        // 科幻/童话/悬疑 等
  tone: string;         // 第一人称/第三人称 等
  length: string;       // short/medium/long
}

/** 生成响应：返回异步任务 ID */
export interface GenerateResult {
  taskId: number;
}

/** 故事分页 */
export interface StoryPage {
  items: Story[];
  nextCursor: string;
  hasMore: boolean;
  total: number;
  page: number;
  size: number;
}

/** WebSocket 消息类型 */
export interface WsMessage {
  type: string;         // chunk | done | ping | error
  data?: string;        // chunk 类型的文本片段
  message?: string;     // error 类型的错误信息
  storyId?: number;     // done 消息时携带
  wordCount?: number;   // done 消息时携带
}
