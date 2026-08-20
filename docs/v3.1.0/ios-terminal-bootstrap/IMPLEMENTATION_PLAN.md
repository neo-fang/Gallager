# CtrlX 3.1.0 Stage 5：iOS Terminal 首帧稳定化

## 问题

iOS Viewer 进入 window 时，会在 Host 完成 terminal stream bootstrap 之前展示
`initialState`。随后到达的启动期增量、终端解析、滚底和软键盘布局动画同时发生，
导致历史内容明显闪烁。启用 `Show Keyboard on Entry` 后，固定延迟的第二次滚底会
进一步拉长不稳定时间。

macOS Viewer 已使用共享的 `TerminalStreamBootstrapPolicy` 和
`TerminalStreamBootstrapBuffer`：快照与启动期增量先离屏合并，收到 Start 确认后
再一次性显示。iOS 尚未接入这套语义。

## 设计

1. iOS `StreamCoordinator` 复用共享 bootstrap policy/buffer。
2. `initialState` 只建立 bootstrap 数据，不提前切换到 `.streaming`。
3. Start 确认到达后，将按顺序合并的尺寸和数据交给一个新的 `TerminalState`。
4. `UIViewRepresentable.makeUIView` 在返回原生视图前同步排空首帧 feed，避免把解析
   中间状态提交到屏幕。
5. 自动键盘只在首帧内容完成后激活。
6. 删除 100ms/350ms 固定延迟；使用一次首帧定位和 `keyboardDidShow` 后的最终定位。

## 非目标

- 不修改 Host、Relay、E2EE 或 terminal stream wire model。
- 不减少 tmux scrollback，不改变终端内容。
- 不缓存离开后的 UIKit terminal，也不让多个 window 常驻内存。
- 不改变手动显示/隐藏键盘、复制页或多 pane 输入归属规则。

## 验收标准

- Start 确认前 terminal 不进入 `.streaming`。
- 初始快照、启动期增量和尺寸变化保持原顺序并在首帧显示前处理完成。
- `Show Keyboard on Entry` 不与 bootstrap 同时争用布局；不再运行固定延迟滚底任务。
- stream replacement、missing initial state、resetState 和实时增量行为不回归。
- 共享测试、Feature 测试和 iPhoneOS 构建通过。
- iPhone 真机验证近期 window、长时间未打开 window，以及默认键盘开/关两种路径。
