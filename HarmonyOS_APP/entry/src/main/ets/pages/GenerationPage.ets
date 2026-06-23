import { WebSocketManager, WsCallbacks } from '../common/net/WebSocketManager';
import { HttpClient } from '../common/net/HttpClient';
import { Story, StoryPage } from '../common/model/Story';
import { router } from '@kit.ArkUI';

/**
 * 生成进度页 — 连接 WebSocket 流式接收故事内容。
 * 路由参数：{ taskId: number, storyId: number }
 * 进入后先查询 story 状态，连接 WS 流式展示。
 */
@Entry
@Component
struct GenerationPage {
  @State content: string = '';
  @State statusText: string = '正在连接...';
  @State progress: number = 0;
  @State wordCount: number = 0;
  @State done: boolean = false;
  @State error: boolean = false;
  private ws: WebSocketManager | null = null;
  private storyId: number = 0;

  aboutToAppear(): void {
    const params = router.getParams() as Record<string, Object>;
    // storyId 可能在参数中直接传入，也可能需要从 task 中获取
    // 这里先尝试直接获取 storyId，如果没有则后续轮询
    const sid = params?.['storyId'] as number ?? 0;
    this.storyId = sid;
    this.pollAndConnect();
  }

  aboutToDisappear(): void {
    // 离开页面时断开 WebSocket
    if (this.ws) {
      this.ws.disconnect();
    }
  }

  /** 轮询获取 storyId，然后连接 WebSocket */
  async pollAndConnect(): Promise<void> {
    // 如果已经有 storyId，直接连接
    if (this.storyId > 0) {
      this.connectWs();
      return;
    }
    // 否则轮询 /stories/my 拿最新一条
    let attempts = 0;
    const maxAttempts = 10;
    const poll = setInterval(async () => {
      try {
        attempts++;
        const result: StoryPage = await HttpClient.get<StoryPage>(`/stories/my?page=1&size=1`);
        if (result.items.length > 0 && result.items[0].status === 'draft') {
          this.storyId = result.items[0].id;
          clearInterval(poll);
          this.connectWs();
        }
        if (attempts >= maxAttempts) {
          clearInterval(poll);
          this.statusText = '获取故事任务超时，请返回重试';
          this.error = true;
        }
      } catch (_e) {
        if (attempts >= maxAttempts) {
          clearInterval(poll);
          this.statusText = '获取故事任务超时，请返回重试';
          this.error = true;
        }
      }
    }, 2000);
  }

  /** 连接 WebSocket 开始流式接收 */
  connectWs(): void {
    const callbacks: WsCallbacks = {
      onChunk: (text: string) => {
        this.content += text;
        this.progress += text.length;
        this.statusText = '正在生成...';
      },
      onDone: (sid: number, wc: number) => {
        this.wordCount = wc;
        this.statusText = '';
        this.done = true;
        this.ws = null;
      },
      onError: (msg: string) => {
        this.statusText = msg;
        this.error = true;
        this.ws = null;
      }
    };
    this.ws = new WebSocketManager(callbacks);
    this.ws.connect(this.storyId);
  }

  build() {
    Column() {
      // 顶部
      Row() {
        if (this.done || this.error) {
          Button('← 返回')
            .type(ButtonType.Capsule)
            .onClick(() => { router.back(); })
        } else {
          Text('生成中').fontSize(18).fontWeight(FontWeight.Bold)
        }
        Blank()
        if (this.done) {
          Text(`共 ${this.wordCount} 字`).fontSize(14).fontColor('#4CAF50')
        }
      }
      .width('100%')
      .padding(16)

      // 状态文字
      if (this.statusText) {
        Text(this.statusText)
          .fontSize(14)
          .fontColor(this.error ? '#FF5252' : '#999')
          .padding({ left: 16, right: 16, bottom: 8 })
      }

      // 内容区
      if (this.content) {
        Scroll() {
          Text(this.content)
            .fontSize(15)
            .lineHeight(24)
            .width('100%')
            .padding(16)
        }
        .layoutWeight(1)
        .backgroundColor(Color.White)
      } else if (!this.error) {
        Column({ space: 12 }) {
          LoadingProgress()
            .width(36)
            .height(36)
            .color('#2196F3')
          Text('AI 正在构思你的故事...')
            .fontSize(14)
            .fontColor('#999')
        }
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
      }

      // 底部操作
      if (this.done) {
        Row({ space: 12 }) {
          Button('查看故事库')
            .type(ButtonType.Capsule)
            .onClick(() => {
              // TODO: Sprint 4 跳转故事库
              router.back();
            })
          Button('再写一篇')
            .type(ButtonType.Capsule)
            .backgroundColor('#4CAF50')
            .onClick(() => {
              router.back();
              router.back();  // 回到 Index 页
            })
        }
        .padding(16)
        .justifyContent(FlexAlign.Center)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }
}
