/**
 * 想法数据模型（后端 /ideas 接口返回结构）。
 * 字段名与后端 Spring Boot 驼峰一致。
 */
export interface Idea {
  id: number;
  userId: number;
  content: string;
  categoryId: number | null;
  archived: boolean;
  pinned: boolean;
  clientUuid: string;
  createdAt: string;
  updatedAt: string;
}

/** 后端分页响应包含的 items */
export interface IdeaPage {
  items: Idea[];
  nextCursor: string;
  hasMore: boolean;
}

/** 创建想法请求体 */
export interface IdeaCreateBody {
  content: string;
  categoryId: number;
  clientUuid: string;
}

/** 批量同步请求 */
export interface SyncBody {
  items: Idea[];
}

/** 同步返回结果 */
export interface SyncResult {
  created: number;
  updated: number;
  conflicts: number;
}
