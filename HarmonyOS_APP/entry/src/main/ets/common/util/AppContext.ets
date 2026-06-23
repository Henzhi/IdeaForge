import { common } from '@kit.AbilityKit';

/**
 * 全局 Application 上下文持有者。
 * HarmonyOS API 24 已废弃 globalThis，必须在 Ability 启动时显式注入。
 * 注意：ArkTS 禁止在静态方法中使用 this，因此用类名直接引用静态字段。
 */
export class AppContext {
  private static ctx: common.UIAbilityContext;

  static set(context: common.UIAbilityContext): void {
    AppContext.ctx = context;
  }

  static get(): common.UIAbilityContext {
    if (!AppContext.ctx) {
      throw new Error('AppContext 未初始化，请先在 EntryAbility.onCreate 中调用 AppContext.set()');
    }
    return AppContext.ctx;
  }
}
