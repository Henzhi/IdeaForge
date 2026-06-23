import { preferences } from '@kit.ArkData';
import { AppContext } from './AppContext';

const PREF_NAME = 'user_prefs';
const KEY_TOKEN = 'satoken';
const KEY_USER_ID = 'user_id';

/**
 * 键值偏好存储工具。用于轻量级数据（Token、用户 ID）的持久化。
 * API 24 中 preferences 的操作需要 try/catch 包裹。
 */
export class PreferencesUtils {

  static async getToken(): Promise<string> {
    try {
      const prefs = await preferences.getPreferences(AppContext.get(), PREF_NAME);
      return prefs.getSync(KEY_TOKEN, '') as string;
    } catch (e) {
      return '';
    }
  }

  static async setToken(token: string): Promise<void> {
    try {
      const prefs = await preferences.getPreferences(AppContext.get(), PREF_NAME);
      await prefs.put(KEY_TOKEN, token);
      await prefs.flush();
    } catch (e) {
      console.error('setToken failed', JSON.stringify(e));
    }
  }

  static async getUserId(): Promise<number> {
    try {
      const prefs = await preferences.getPreferences(AppContext.get(), PREF_NAME);
      return prefs.getSync(KEY_USER_ID, -1) as number;
    } catch (e) {
      return -1;
    }
  }

  static async setUserId(id: number): Promise<void> {
    try {
      const prefs = await preferences.getPreferences(AppContext.get(), PREF_NAME);
      await prefs.put(KEY_USER_ID, id);
      await prefs.flush();
    } catch (e) {
      console.error('setUserId failed', JSON.stringify(e));
    }
  }

  static async clear(): Promise<void> {
    try {
      const prefs = await preferences.getPreferences(AppContext.get(), PREF_NAME);
      await prefs.delete(KEY_TOKEN);
      await prefs.delete(KEY_USER_ID);
      await prefs.flush();
    } catch (e) {
      console.error('clear failed', JSON.stringify(e));
    }
  }
}
