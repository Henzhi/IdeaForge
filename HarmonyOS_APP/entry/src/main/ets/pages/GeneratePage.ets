import { HttpClient } from '../common/net/HttpClient';
import { Idea, IdeaPage } from '../common/model/Idea';
import { GenerateBody, GenerateResult } from '../common/model/Story';
import { router } from '@kit.ArkUI';

/** 风格选项 */
interface StyleOption {
  label: string;
  value: string;
}

/** 路由参数 */
interface GenParams {
  taskId: number;
  storyId: number;
}

/**
 * 生成配置页 — 选择想法 + 风格/人称/篇幅参数，提交生成任务。
 */
@Entry
@Component
struct GeneratePage {
  @State ideas: Idea[] = [];
  @State selectedIds: number[] = [];
  @State style: string = '童话';
  @State tone: string = '第一人称';
  @State length: string = 'medium';
  @State submitting: boolean = false;

  private styles: StyleOption[] = [
    { label: '童话', value: '童话' },
    { label: '科幻', value: '科幻' },
    { label: '悬疑', value: '悬疑' },
    { label: '奇幻', value: '奇幻' },
    { label: '现实', value: '现实' },
    { label: '古风', value: '古风' }
  ];
  private tones: StyleOption[] = [
    { label: '第一人称', value: '第一人称' },
    { label: '第三人称', value: '第三人称' }
  ];
  private lengths: StyleOption[] = [
    { label: '短篇 (~500字)', value: 'short' },
    { label: '中篇 (~1500字)', value: 'medium' },
    { label: '长篇 (~3000字)', value: 'long' }
  ];

  aboutToAppear(): void {
    this.loadIdeas();
  }

  async loadIdeas(): Promise<void> {
    try {
      const result: IdeaPage = await HttpClient.get<IdeaPage>(`/ideas?limit=50`);
      this.ideas = result.items;
    } catch (e) {
      console.error('加载想法失败', (e as Error).message);
    }
  }

  // ===== 多选切换 =====
  toggleSelect(id: number): void {
    const idx = this.selectedIds.indexOf(id);
    if (idx >= 0) {
      this.selectedIds.splice(idx, 1);
    } else {
      this.selectedIds.push(id);
    }
  }

  // ===== 提交生成 =====
  async submitGenerate(): Promise<void> {
    if (this.selectedIds.length === 0) {
      return;
    }
    this.submitting = true;
    try {
      const body: GenerateBody = {
        ideaIds: this.selectedIds,
        style: this.style,
        tone: this.tone,
        length: this.length
      };
      const result: GenerateResult = await HttpClient.post<GenerateResult>('/stories/generate', body);
      // 跳转到生成进度页
      const genParams: GenParams = { taskId: result.taskId, storyId: 0 };
      router.pushUrl({ url: 'pages/GenerationPage', params: genParams as Object });
    } catch (e) {
      console.error('提交生成失败', (e as Error).message);
    } finally {
      this.submitting = false;
    }
  }

  build() {
    Column() {
      // 顶部
      Row() {
        Button('← 返回')
          .type(ButtonType.Capsule)
          .onClick(() => { router.back(); })
        Blank()
        Text('创建故事').fontSize(18).fontWeight(FontWeight.Bold)
        Blank()
        Text('').width(60)  // 占位对齐
      }
      .width('100%')
      .padding(16)

      Column({ space: 12 }) {
        // 选择想法
        Text('选择灵感').fontSize(14).fontColor('#666').width('100%')
        List({ space: 8 }) {
          ForEach(this.ideas, (item: Idea) => {
            ListItem() {
              Row() {
                Text(item.content)
                  .maxLines(2)
                  .fontSize(14)
                  .layoutWeight(1)
                  .textOverflow({ overflow: TextOverflow.Ellipsis })
                Checkbox()
                  .select(this.selectedIds.includes(item.id))
                  .onChange(() => { this.toggleSelect(item.id); })
              }
              .padding(12)
              .backgroundColor(Color.White)
              .borderRadius(8)
            }
          })
        }
        .height(180)
        .padding({ left: 16, right: 16 })

        // 参数选择
        Text('风格').fontSize(14).fontColor('#666')
        Row() {
          ForEach(this.styles, (s: StyleOption) => {
            Button(s.label)
              .type(ButtonType.Capsule)
              .fontSize(12)
              .margin({ right: 6 })
              .backgroundColor(this.style === s.value ? '#2196F3' : '#E0E0E0')
              .fontColor(this.style === s.value ? Color.White : '#333')
              .onClick(() => { this.style = s.value; })
          })
        }
        .padding({ left: 16, right: 16 })

        Text('叙事人称').fontSize(14).fontColor('#666')
        Row() {
          ForEach(this.tones, (t: StyleOption) => {
            Button(t.label)
              .type(ButtonType.Capsule)
              .fontSize(12)
              .margin({ right: 6 })
              .backgroundColor(this.tone === t.value ? '#2196F3' : '#E0E0E0')
              .fontColor(this.tone === t.value ? Color.White : '#333')
              .onClick(() => { this.tone = t.value; })
          })
        }
        .padding({ left: 16, right: 16 })

        Text('篇幅').fontSize(14).fontColor('#666')
        Row() {
          ForEach(this.lengths, (l: StyleOption) => {
            Button(l.label)
              .type(ButtonType.Capsule)
              .fontSize(12)
              .margin({ right: 6 })
              .backgroundColor(this.length === l.value ? '#2196F3' : '#E0E0E0')
              .fontColor(this.length === l.value ? Color.White : '#333')
              .onClick(() => { this.length = l.value; })
          })
        }
        .padding({ left: 16, right: 16 })
      }
      .width('100%')

      Blank()

      // 提交按钮
      Button(this.submitting ? '提交中...' : `开始生成 (已选 ${this.selectedIds.length} 条)`)
        .width('90%')
        .height(44)
        .enabled(this.selectedIds.length > 0 && !this.submitting)
        .backgroundColor(this.selectedIds.length > 0 ? '#4CAF50' : '#BDBDBD')
        .onClick(() => { this.submitGenerate(); })
        .margin({ bottom: 24 })
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#F5F5F5')
  }
}
