# Stage 15 TODO：iOS 终端底部输入控件

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 0/5 tasks
- **Dependencies**: Stage 11 ✅, Stage 14 ✅

## Tasks

- [ ] 新增共享的 iOS 底部键盘控件。
- [ ] 将单终端与隐藏导航栏场景迁移到底部安全区。
- [ ] 将多 pane 页迁移到父视图底部控件，保持选中 pane 输入语义。
- [ ] 更新 E2E 可访问性等待点并完成聚焦回归测试。
- [ ] 完成 iOS device 构建、签名、覆盖安装和 iPhone 真机验收。

## Decisions

- 键盘入口不绑定到终端内容区的单击、双击或长按。
- 底部控件使用 SwiftUI 原生 safe-area inset，不手工监听键盘高度。
- 复制入口和复制 sheet 的焦点策略保持不变。
- 本阶段只修改 iOS UI，不更新 macOS DMG。

## Blockers

- None.

## Verification

- Pending.
