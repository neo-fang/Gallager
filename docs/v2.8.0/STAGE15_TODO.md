# Stage 15 TODO：iOS 终端输入控件位置

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 5/6 tasks
- **Dependencies**: Stage 11 ✅, Stage 14 ✅

## Tasks

- [x] 新增共享的 iOS 底部键盘控件。
- [x] 新增持久化位置枚举和 Settings 两项 Picker，默认右上角。
- [x] 单终端与隐藏导航栏场景根据设置切换两种位置。
- [x] 多 pane 页由父视图根据设置只显示一个位置，保持选中 pane 输入语义。
- [x] 更新位置策略测试和 E2E 可访问性等待点，完成回归测试。
- [ ] 完成 iOS device 构建、签名、覆盖安装和 iPhone 真机验收。

## Decisions

- 键盘入口不绑定到终端内容区的单击、双击或长按。
- 位置使用一个枚举选项，不使用两个可能冲突的 Boolean 开关。
- 默认右上角以保持升级用户的原有行为；底部使用者显式切换。
- 底部控件使用 SwiftUI 原生 safe-area inset，不手工监听键盘高度。
- 复制入口和复制 sheet 的焦点策略保持不变。
- 本阶段只修改 iOS UI，不更新 macOS DMG。

## Blockers

- None.

## Verification

- `TerminalKeyboardControlPositionTests`：2 tests / 1 suite 通过。
- 完整 Swift package：1,590 tests / 221 suites 通过。
- 可配置位置实现的 iOS device `Debug` 无签名构建通过。
- `ZengJice iPhone` 当前为 `unavailable`，待设备恢复连接后完成签名、安装和真机验收。
