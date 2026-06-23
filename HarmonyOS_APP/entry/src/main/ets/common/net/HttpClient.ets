import { http } from '@kit.NetworkKit';
import { PreferencesUtils } from '../util/PreferencesUtils';

// 本地开发用 10.0.2.2 映射宿主机；生产部署需替换为真实域名
const BASE_URL = 'http://10.0.2.2:8080/api/v1';

/** 后端统一响应结构 */
export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

/**
 * 统一网络请求客户端。
 * - 自动从 Preferences 读取 token 并注入 satoken 请求头
 * - 检查业务码 code，非 200 时抛出异常
 * - 每次请求后销毁 http 对象，避免连接泄漏
 */
export class HttpClient {

  // 内部构建请求头（ArkTS 要求静态方法中不能使用 this，拆为独立函数）
  private static async buildHeaders(): Promise<Record<string, string>> {
    const token = await PreferencesUtils.getToken();
    const headers: Record<string, string> = { 'Content-Type': 'application/json' };
    if (token) {
      headers['satoken'] = token;
    }
    return headers;
  }

  static async get<T>(path: string, params?: Object): Promise<T> {
    let url = BASE_URL + path;
    if (params) {
      const paramsObj = params as Record<string, Object>;
      const query = Object.keys(paramsObj)
        .map(k => `${k}=${encodeURIComponent(String(paramsObj[k]))}`)
        .join('&');
      url += '?' + query;
    }
    return HttpClient.request<T>(http.RequestMethod.GET, url);
  }

  static async post<T>(path: string, body?: Object): Promise<T> {
    return HttpClient.request<T>(http.RequestMethod.POST, BASE_URL + path,
      body ? JSON.stringify(body) : undefined);
  }

  static async put<T>(path: string, body?: Object): Promise<T> {
    return HttpClient.request<T>(http.RequestMethod.PUT, BASE_URL + path,
      body ? JSON.stringify(body) : undefined);
  }

  /** PUT 方法，参数拼接为 URL 查询字符串（适用于 @RequestParam 风格的后端） */
  static async putQuery<T>(path: string, params?: Object): Promise<T> {
    let url = BASE_URL + path;
    if (params) {
      const paramsObj = params as Record<string, Object>;
      const query = Object.keys(paramsObj)
        .map(k => `${k}=${encodeURIComponent(String(paramsObj[k]))}`)
        .join('&');
      url += '?' + query;
    }
    return HttpClient.request<T>(http.RequestMethod.PUT, url);
  }

  static async delete<T>(path: string): Promise<T> {
    return HttpClient.request<T>(http.RequestMethod.DELETE, BASE_URL + path);
  }

  /** 统一请求入口 */
  private static async request<T>(method: http.RequestMethod, url: string, body?: string): Promise<T> {
    const httpReq = http.createHttp();
    try {
      const response = await httpReq.request(url, {
        method: method,
        header: await HttpClient.buildHeaders(),
        extraData: body,
        connectTimeout: 10000,
        readTimeout: 30000,
        expectDataType: http.HttpDataType.STRING
      });
      const result: ApiResult<T> = JSON.parse(response.result as string) as ApiResult<T>;
      if (result.code !== 200) {
        throw new Error(`[${result.code}] ${result.message}`);
      }
      return result.data;
    } catch (e) {
      // 网络错误 或 JSON 解析错误统一转为有意义的消息
      const msg = (e as Error).message || '';
      if (msg.startsWith('[')) {
        throw new Error(msg);  // ArkTS 要求 throw 的必须是 Error 实例
      }
      throw new Error('网络请求失败，请检查网络连接');
    } finally {
      httpReq.destroy();
    }
  }
}
