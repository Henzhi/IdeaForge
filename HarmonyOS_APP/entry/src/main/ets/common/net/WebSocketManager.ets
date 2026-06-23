import { webSocket } from '@kit.NetworkKit';
import { util } from '@kit.ArkTS';
import { PreferencesUtils } from '../util/PreferencesUtils';
import { WsMessage } from '../model/Story';

// 本地开发用；生产环境替换为真实域名
const WS_BASE = 'ws://10.0.2.2:8080';

/** 回调接口 */
export interface WsCallbacks {
  onChunk: (text: string) => void;
  onDone: (storyId: number, wordCount: number) => void;
  onError: (msg: string) => void;
}

/**
 * WebSocket 管理器 — 连接后端流式故事生成。
 * 后端端点：/ws?satoken=<login_token>
 * 直接使用 Sa-Token 登录凭证，无需额外获取一次性 token。
 */
export class WebSocketManager {
  private ws: webSocket.WebSocket | null = null;
  private callbacks: WsCallbacks;
  private pingTimer: number = 0;

  constructor(callbacks: WsCallbacks) {
    this.callbacks = callbacks;
  }

  /** 连接故事流 */
  async connect(_storyId: number): Promise<void> {
    try {
      const satoken = await PreferencesUtils.getToken();
      if (!satoken) {
        this.callbacks.onError('未登录，请重新登录');
        return;
      }
      const url = `${WS_BASE}/ws?satoken=${satoken}`;
      this.ws = webSocket.createWebSocket();

      this.ws.on('open', (_err: Object, _value: Object) => {
        this.pingTimer = setInterval(() => {
          if (this.ws) {
            const pong: WsMessage = { type: 'pong' };
            this.ws.send(JSON.stringify(pong));
          }
        }, 30000);
      });

      this.ws.on('message', (err: Object, value: Object) => {
        if (typeof value === 'string') {
          this.handleMessage(value);
        } else {
          const buf: ArrayBuffer = value as ArrayBuffer;
          const text = new util.TextDecoder().decode(new Uint8Array(buf));
          this.handleMessage(text);
        }
      });

      this.ws.on('error', (err: Object) => {
        this.cleanup();
        this.callbacks.onError('WebSocket 连接错误');
      });

      this.ws.on('close', (result: Object) => {
        this.cleanup();
      });

      this.ws.connect(url, (err: Object) => {
        if (err) {
          this.callbacks.onError('WebSocket 连接失败');
        }
      });
    } catch (e) {
      this.callbacks.onError(`连接失败: ${(e as Error).message}`);
    }
  }

  /** 断开连接 */
  disconnect(): void {
    this.cleanup();
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }

  private handleMessage(text: string): void {
    try {
      const msg: WsMessage = JSON.parse(text) as WsMessage;
      switch (msg.type) {
        case 'chunk':
          if (msg.data) {
            this.callbacks.onChunk(msg.data);
          }
          break;
        case 'done':
          this.cleanup();
          this.callbacks.onDone(msg.storyId ?? 0, msg.wordCount ?? 0);
          break;
        case 'error':
          this.cleanup();
          this.callbacks.onError(msg.message ?? '生成失败');
          break;
        case 'ping':
          if (this.ws) {
            const pong: WsMessage = { type: 'pong' };
            this.ws.send(JSON.stringify(pong));
          }
          break;
        default:
          break;
      }
    } catch (_e) {
      // 非 JSON 消息忽略
    }
  }

  private cleanup(): void {
    if (this.pingTimer) {
      clearInterval(this.pingTimer);
      this.pingTimer = 0;
    }
  }
}
