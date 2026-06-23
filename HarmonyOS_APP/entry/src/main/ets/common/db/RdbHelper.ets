import { relationalStore } from '@kit.ArkData';
import { AppContext } from '../util/AppContext';
import { Idea } from '../model/Idea';

/**
 * 关系型数据库助手 — 想法本地离线缓存。
 * 网络不可用时可直接读取本地数据，恢复后通过 sync 同步。
 */
const SQL_CREATE_IDEA = `CREATE TABLE IF NOT EXISTS idea (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  server_id INTEGER,
  user_id INTEGER,
  content TEXT NOT NULL,
  category_id INTEGER,
  client_uuid TEXT UNIQUE,
  is_archived INTEGER DEFAULT 0,
  is_pinned INTEGER DEFAULT 0,
  created_at TEXT,
  updated_at TEXT,
  sync_status INTEGER DEFAULT 0
)`;

export class RdbHelper {
  private static store?: relationalStore.RdbStore;

  /** 获取单例数据库实例 */
  static async getStore(): Promise<relationalStore.RdbStore> {
    if (RdbHelper.store) {
      return RdbHelper.store;
    }
    const config: relationalStore.StoreConfig = {
      name: 'ideaforge.db',
      securityLevel: relationalStore.SecurityLevel.S1
    };
    RdbHelper.store = await relationalStore.getRdbStore(AppContext.get(), config);
    await RdbHelper.store.executeSql(SQL_CREATE_IDEA);
    return RdbHelper.store;
  }

  /** 批量插入/更新缓存的想法 */
  static async upsertIdeas(ideas: Idea[]): Promise<void> {
    const store = await RdbHelper.getStore();
    const valueBucket: relationalStore.ValuesBucket[] = ideas.map(i => ({
      'server_id': i.id,
      'user_id': i.userId,
      'content': i.content,
      'category_id': i.categoryId,
      'client_uuid': i.clientUuid,
      'is_archived': i.archived ? 1 : 0,
      'is_pinned': i.pinned ? 1 : 0,
      'created_at': i.createdAt,
      'updated_at': i.updatedAt,
      'sync_status': 1
    }));
    // 先清旧数据再批量写入，简单策略（后续可改为 merge）
    await store.executeSql('DELETE FROM idea WHERE user_id = ?', [String(ideas[0]?.userId ?? 1)]);
    await store.batchInsert('idea', valueBucket);
  }

  /** 读取本地缓存的想法 */
  static async getLocalIdeas(): Promise<Idea[]> {
    const store = await RdbHelper.getStore();
    const predicates = new relationalStore.RdbPredicates('idea');
    predicates.orderByDesc('created_at');
    const result = await store.query(predicates);
    const ideas: Idea[] = [];
    while (result.goToNextRow()) {
      ideas.push({
        id: result.getLong(result.getColumnIndex('server_id')),
        userId: result.getLong(result.getColumnIndex('user_id')),
        content: result.getString(result.getColumnIndex('content')),
        categoryId: result.isColumnNull(result.getColumnIndex('category_id'))
          ? null : result.getLong(result.getColumnIndex('category_id')),
        archived: result.getLong(result.getColumnIndex('is_archived')) === 1,
        pinned: result.getLong(result.getColumnIndex('is_pinned')) === 1,
        clientUuid: result.getString(result.getColumnIndex('client_uuid')),
        createdAt: result.getString(result.getColumnIndex('created_at')),
        updatedAt: result.getString(result.getColumnIndex('updated_at')),
      });
    }
    result.close();
    return ideas;
  }
}
