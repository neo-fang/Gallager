# Stage 15 TODO：iOS 终端底部输入控件

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 4/5 tasks
- **Dependencies**: Stage 11 ✅, Stage 14 ✅

## Tasks

- [x] 新增共享的 iOS 底部键盘控件。
- [x] 将单终端与隐藏导航栏场景迁移到底部安全区。
- [x] 将多 pane 页迁移到父视图底部控件，保持选中 pane 输入语义。
- [x] 更新 E2E 可访问性等待点并完成聚焦回归测试。
- [ ] 完成 iOS device 构建、签名、覆盖安装和 iPhone 真机验收。

## Decisions

- 键盘入口不绑定到终端内容区的单击、双击或长按。
- 底部控件使用 SwiftUI 原生 safe-area inset，不手工监听键盘高度。
- 复制入口和复制 sheet 的焦点策略保持不变。
- 本阶段只修改 iOS UI，不更新 macOS DMG。

## Blockers

- None.

## Verification

- `TerminalInputPresentationTests`：3 tests / 1 suite 通过。
- 完整 Swift package：1,588 tests / 220 suites 通过。
- iOS device `Debug` 无签名构建通过，底部控件、单终端和多 pane 路径均已编译。
- `ZengJice iPhone` 当前为 `unavailable`，待设备恢复连接后完成签名、安装和真机验收。
