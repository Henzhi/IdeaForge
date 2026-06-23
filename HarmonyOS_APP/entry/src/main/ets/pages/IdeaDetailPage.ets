import { HttpClient } from '../common/net/HttpClient';
import { Idea } from '../common/model/Idea';
import { router } from '@kit.ArkUI';

/** 更新想法请求参数 */
interface UpdateIdeaParams {
  content: string;
}

/**
 * 想法详情页 — 查看/编辑/归档/置顶/删除。
 * 接收路由参数 params.id。
 */
@Entry
@Component
struct IdeaDetailPage {
  @State idea: Idea | null = null;
  @State editContent: string = '';
  @State editing: boolean = false;
  @State loading: boolean = true;       // 初始为 true，加载完成后变 false
  @State loadError: string = '';        // 加载失败时显示错误信息，非空表示加载失败
  @State saving: boolean = false;       // 保存中状态
  @State saveError: string = '';        // 保存失败提示
  private ideaId: number = 0;

  aboutToAppear(): void {
    const params = router.getParams() as Record<string, Object>;
    this.ideaId = params?.['id'] as number ?? 0;
    this.loadIdea();
  }

  async loadIdea(): Promise<void> {
    this.loading = true;
    this.loadError = '';
    try {
      this.idea = await HttpClient.get<Idea>(`/ideas/${this.ideaId}`);
      this.editContent = this.idea.content;
    } catch (e) {
      const msg = (e as Error).message || '未知错误';
      // 后端返回业务码（如 1201 «想法不存在»），提取可读信息
      this.loadError = msg.startsWith('[') ? msg.substring(msg.indexOf(']') + 1).trim() : '加载失败，请返回重试';
      console.error('加载想法失败', msg);
    } finally {
      this.loading = false;
    }
  }

  // ===== 编辑 =====

  startEdit(): void {
    this.editContent = this.idea?.content ?? '';
    this.saveError = '';
    this.editing = true;
  }

  async saveEdit(): Promise<void> {
    if (!this.editContent.trim()) {
      this.saveError = '内容不能为空';
      return;
    }
    this.saving = true;
    this.saveError = '';
    try {
      // 后端 PUT /ideas/{id} 使用 @RequestParam 而非 @RequestBody
      const updateParams: UpdateIdeaParams = { content: this.editContent };
      this.idea = await HttpClient.putQuery<Idea>(`/ideas/${this.ideaId}`, updateParams);
      this.editing = false;
    } catch (e) {
      const msg = (e as Error).message || '保存失败';
      this.saveError = msg.startsWith('[') ? msg.substring(msg.indexOf(']') + 1).trim() : msg;
      console.error('保存失败', msg);
    } finally {
      this.saving = false;
    }
  }

  cancelEdit(): void {
    this.editContent = this.idea?.content ?? '';
    this.editing = false;
  }

  // ===== 归档/置顶 =====

  async toggleArchive(): Promise<void> {
    if (!this.idea) {
      return;
    }
    const newArchived = !this.idea.archived;
    try {
      await HttpClient.post<void>(`/ideas/${this.ideaId}/archive?archived=${newArchived}`);
      this.idea.archived = newArchived;
    } catch (e) {
      console.error('操作失败', (e as Error).message);
    }
  }

  async togglePin(): Promise<void> {
    if (!this.idea) {
      return;
    }
    const newPinned = !this.idea.pinned;
    try {
      await HttpClient.post<void>(`/ideas/${this.ideaId}/pin?pinned=${newPinned}`);
      this.idea.pinned = newPinned;
    } catch (e) {
      console.error('操作失败', (e as Error).message);
    }
  }

  // ===== 删除 =====

  async deleteIdea(): Promise<void> {
    try {
      await HttpClient.delete<void>(`/ideas/${this.ideaId}`);
      router.back();
    } catch (e) {
      console.error('删除失败', (e as Error).message);
    }
  }

  build() {
    Column() {
      if (this.loading) {
        // 加载中状态
        Text('加载中...').fontSize(16).fontColor('#999')
      } else if (this.loadError) {
        // 加载失败 (如已被删除)
        Column({ space: 12 }) {
          Text('加载失败').fontSize(18).fontWeight(FontWeight.Bold)
          Text(this.loadError).fontSize(14).fontColor('#999')
          Button('返回列表').type(ButtonType.Capsule).onClick(() => { router.back(); })
        }
      } else if (!this.idea) {
        // 兜底：数据为空
        Text('暂无数据').fontSize(16).fontColor('#999')
      } else {
        // 正常内容
        Column({ space: 12 }) {
          // 顶部操作栏
          Row() {
            Button('← 返回')
              .type(ButtonType.Capsule)
              .onClick(() => { router.back(); })

            Blank()

            // 归档按钮
            Button(this.idea.archived ? '取消归档' : '归档')
              .type(ButtonType.Capsule)
              .fontSize(12)
              .backgroundColor(this.idea.archived ? '#E0E0E0' : '#FFF3E0')
              .onClick(() => { this.toggleArchive(); })

            // 置顶按钮
            Button(this.idea.pinned ? '取消置顶' : '置顶')
              .type(ButtonType.Capsule)
              .fontSize(12)
              .backgroundColor(this.idea.pinned ? '#E0E0E0' : '#E8F5E9')
              .margin({ left: 8 })
              .onClick(() => { this.togglePin(); })
          }
          .width('100%')
          .padding(16)

          // 内容区
          if (this.editing) {
            // 编辑模式
            Column({ space: 12 }) {
              TextArea({ text: this.editContent })
                .height(200)
                .fontSize(15)
                .onChange((value: string) => { this.editContent = value; })

              // 错误提示
              if (this.saveError) {
                Text(this.saveError).fontSize(12).fontColor('#FF5252')
              }

              Row({ space: 8 }) {
                Button(this.saving ? '保存中...' : '保存')
                  .type(ButtonType.Capsule)
                  .enabled(!this.saving)
                  .onClick(() => { this.saveEdit(); })
                Button('取消')
                  .type(ButtonType.Capsule)
                  .backgroundColor('#E0E0E0')
                  .enabled(!this.saving)
                  .onClick(() => { this.cancelEdit(); })
              }
            }
            .padding(16)
          } else {
            // 查看模式
            Column({ space: 12 }) {
              Text(this.idea.content)
                .fontSize(16)
                .width('100%')
                .padding(16)
                .backgroundColor(Color.White)
                .borderRadius(12)
                .onClick(() => { this.startEdit(); })

              // 元信息
              Column({ space: 4 }) {
                Text(`创建时间: ${this.idea.createdAt.substring(0, 19).replace('T', ' ')}`)
                  .fontSize(12).fontColor('#999')
                Text(`更新时间: ${this.idea.updatedAt.substring(0, 19).replace('T', ' ')}`)
                  .fontSize(12).fontColor('#999')
              }
              .alignItems(HorizontalAlign.Start)
              .width('100%')
              .padding(12)
            }
            .padding({ left: 16, right: 16 })

            // 编辑按钮
            Button('编辑内容')
              .width('90%')
              .type(ButtonType.Capsule)
              .onClick(() => { this.startEdit(); })
          }

          Blank()

          // 底部删除
          Button('删除此想法')
            .width('90%')
            .backgroundColor('#FF5252')
            .type(ButtonType.Capsule)
            .margin({ bottom: 24 })
            .onClick(() => { this.deleteIdea(); })
        }
        .width('100%')
        .height('100%')
        .backgroundColor('#F5F5F5')
      }
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
  }
}
