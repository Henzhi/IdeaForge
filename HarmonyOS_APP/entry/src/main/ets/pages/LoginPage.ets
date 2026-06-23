import { HttpClient } from '../common/net/HttpClient';
import { PreferencesUtils } from '../common/util/PreferencesUtils';
import { LoginResult } from '../common/model/User';
import { router } from '@kit.ArkUI';

/** 登录请求体（ArkTS 要求对象字面量有显式类型） */
interface LoginBody {
  phone: string;
  password: string;
}

@Entry
@Component
struct LoginPage {
  @State phone: string = '';
  @State password: string = '';
  @State loading: boolean = false;
  @State errorMsg: string = '';

  async login(): Promise<void> {
    if (!this.phone || !this.password) {
      this.errorMsg = '请输入手机号和密码';
      return;
    }
    this.loading = true;
    this.errorMsg = '';
    try {
      const body: LoginBody = { phone: this.phone, password: this.password };
      const result: LoginResult = await HttpClient.post<LoginResult>('/users/login/phone', body);
      await PreferencesUtils.setToken(result.token);
      await PreferencesUtils.setUserId(result.user.id);
      router.replaceUrl({ url: 'pages/Index' });
    } catch (e) {
      this.errorMsg = (e as Error).message;
    } finally {
      this.loading = false;
    }
  }

  build() {
    Column({ space: 16 }) {
      Text('IdeaForge')
        .fontSize(28)
        .fontWeight(FontWeight.Bold)
        .margin({ top: 80 })

      Text('记录灵感 · 生成故事')
        .fontSize(14)
        .fontColor('#888')
        .margin({ bottom: 32 })

      TextInput({ placeholder: '手机号', text: this.phone })
        .type(InputType.Normal)
        .maxLength(11)
        .onChange((value: string) => { this.phone = value; })

      TextInput({ placeholder: '密码', text: this.password })
        .type(InputType.Password)
        .onChange((value: string) => { this.password = value; })

      if (this.errorMsg) {
        Text(this.errorMsg)
          .fontColor('#F44336')
          .fontSize(13)
      }

      Button(this.loading ? '登录中...' : '登录')
        .width('100%')
        .enabled(!this.loading)
        .onClick(() => { this.login(); })

      // Sprint 2 实现注册功能
      // Button('注册新账号').buttonStyle(ButtonStyleMode.TEXTUAL).onClick(() => { router.pushUrl({ url: 'pages/RegisterPage' }); })
    }
    .width('100%')
    .height('100%')
    .padding(32)
    .justifyContent(FlexAlign.Center)
  }
}
