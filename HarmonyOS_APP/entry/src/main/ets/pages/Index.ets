import { HttpClient } from '../common/net/HttpClient';
import { Idea, IdeaPage, IdeaCreateBody } from '../common/model/Idea';
import { router } from '@kit.ArkUI';

/** 加载参数 */
interface LoadParams {
  limit: number;
  cursor?: string;
  archived?: string;
}

/** 路由参数 */
interface RouterParams {
  id: number;
}

@Entry
@Component
struct Index {
  @State ideas: Idea[] = [];
  @State newContent: string = '';
  @State loading: boolean = false;
  @State nextCursor: string | null = null;
  @State hasMore: boolean = false;
  @State showArchived: boolean = false;
  @State currentTab: number = 0;

  onPageShow(): void {
    this.loadIdeas(false);
  }

  switchTab(archived: boolean): void {
    if (this.showArchived === archived) {
      return;
    }
    this.showArchived = archived;
    this.ideas = [];
    this.nextCursor = null;
    this.hasMore = false;
    this.loadIdeas(false);
  }

  async loadIdeas(loadMore: boolean): Promise<void> {
    if (this.loading) {
      return;
    }
    this.loading = true;
    try {
      const params: LoadParams = {
        limit: 20,
        archived: this.showArchived ? 'true' : 'false'
      };
      if (loadMore && this.nextCursor) {
        params.cursor = this.nextCursor;
      }
      const data: IdeaPage = await HttpClient.get<IdeaPage>('/ideas', params);
      if (loadMore) {
        this.ideas = this.ideas.concat(data.items);
      } else {
        this.ideas = data.items;
      }
      this.nextCursor = data.nextCursor;
      this.hasMore = data.hasMore;
    } catch (e) {
      console.error('加载想法失败', (e as Error).message);
    } finally {
      this.loading = false;
    }
  }

  async createIdea(): Promise<void> {
    const text = this.newContent.trim();
    if (!text) {
      return;
    }
    const clientUuid = Date.now().toString(36) + Math.random().toString(36).substring(2, 10);
    try {
      const body: IdeaCreateBody = { content: text, categoryId: 1, clientUuid: clientUuid };
      await HttpClient.post<Idea>('/ideas', body);
      this.newContent = '';
      this.loadIdeas(false);
    } catch (e) {
      console.error('创建失败', (e as Error).message);
    }
  }

  goDetail(idea: Idea): void {
    const routerParams: RouterParams = { id: idea.id };
    router.pushUrl({ url: 'pages/IdeaDetailPage', params: routerParams as Object });
  }

  // ===== 灵感列表内容 =====
  @Builder IdeaListTab() {
    Column() {
      // 标签切换
      Row() {
        Button('活跃')
          .type(ButtonType.Capsule).fontSize(13)
          .backgroundColor(this.showArchived ? '#E0E0E0' : '#2196F3')
          .fontColor(this.showArchived ? '#666' : Color.White)
          .onClick(() => { this.switchTab(false); })
        Button('已归档')
          .type(ButtonType.Capsule).fontSize(13).margin({ left: 8 })
          .backgroundColor(this.showArchived ? '#2196F3' : '#E0E0E0')
          .fontColor(this.showArchived ? Color.White : '#666')
          .onClick(() => { this.switchTab(true); })
      }
      .width('100%').justifyContent(FlexAlign.Center).padding({ bottom: 12 })

      // 想法列表
      List({ space: 12 }) {
        ForEach(this.ideas, (item: Idea) => {
          ListItem() {
            Column() {
              Text(item.content).maxLines(3).fontSize(15)
                .textOverflow({ overflow: TextOverflow.Ellipsis })
              Row() {
                Text(item.createdAt.substring(0, 10)).fontSize(12).fontColor('#999')
                Blank()
                if (item.pinned) { Text('📌').fontSize(14) }
              }
              .width('100%').margin({ top: 8 })
            }
            .padding(16).backgroundColor(Color.White).borderRadius(12)
            .onClick(() => { this.goDetail(item); })
          }
        })
        if (this.hasMore) {
          ListItem() {
            Row() {
              Button(this.loading ? '加载中...' : '加载更多')
                .type(ButtonType.Capsule).enabled(!this.loading)
                .onClick(() => { this.loadIdeas(true); })
            }
            .width('100%').justifyContent(FlexAlign.Center).padding(12)
          }
        }
      }
      .layoutWeight(1).padding({ left: 16, right: 16 }).backgroundColor('#F5F5F5')
      .onReachEnd(() => {
        if (this.hasMore && !this.loading) {
          this.loadIdeas(true);
        }
      })

      // 底部输入栏
      if (!this.showArchived) {
        Row() {
          TextInput({ placeholder: '记录一闪而过的灵感...', text: this.newContent })
            .layoutWeight(1)
            .onChange((value: string) => { this.newContent = value; })
            .onSubmit(() => { this.createIdea(); })
          Button('记录').type(ButtonType.Capsule).margin({ left: 8 })
            .onClick(() => { this.createIdea(); })
        }
        .padding(16).backgroundColor(Color.White)
      }
      if (this.showArchived && !this.loading && this.ideas.length === 0) {
        Text('暂无已归档的想法').fontSize(14).fontColor('#999').padding(16)
      }
    }
  }

  @Builder PlaceholderTab(text: string, route: string) {
    Column() {
      Text(text).fontSize(28).fontColor('#CCC')
      Blank()
      Button('点击进入').type(ButtonType.Capsule).margin({ bottom: 100 })
        .onClick(() => { router.pushUrl({ url: route }); })
    }
    .justifyContent(FlexAlign.Center).width('100%').height('100%')
  }

  build() {
    Tabs({ index: this.currentTab }) {
      TabContent() {
        Column() {
          // 标题
          Row() {
            Text('我的灵感').fontSize(20).fontWeight(FontWeight.Bold)
            Blank()
            Image($r('sys.media.ohos_ic_public_add')).width(24).height(24)
              .onClick(() => { router.pushUrl({ url: 'pages/GeneratePage' }); })
          }
          .width('100%').padding(16)

          this.IdeaListTab()
        }
      }
      .tabBar('灵感')

      TabContent() {
        Column() {
          Text('我的故事').fontSize(20).fontWeight(FontWeight.Bold)
            .width('100%').padding(16)
          Text('故事库即将上线').fontSize(16).fontColor('#CCC')
          Blank()
        }
        .justifyContent(FlexAlign.Center).width('100%').height('100%')
      }
      .tabBar('故事')
    }
    .barHeight(56)
    .barPosition(BarPosition.End)
    .onChange((index: number) => {
      this.currentTab = index;
    })
  }
}
