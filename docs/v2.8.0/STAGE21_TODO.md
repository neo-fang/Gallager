# Stage 21 TODO：iOS 底部输入按钮栏紧凑化

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 1/5 tasks
- **Dependencies**: Stage 15 ✅

## Tasks

- [x] 定位共享底部按钮栏及单 pane/多 pane 调用边界。
- [ ] 将可见按钮栏高度从约 56pt 压缩到约 28pt。
- [ ] 保留键盘显隐、禁用状态、设置切换与无障碍语义。
- [ ] 完成聚焦测试和 iOS 构建验证。
- [ ] 完成 iPhone 真机视觉与点击验收。

## Decisions

- 只修改 `TerminalKeyboardBar`，不在调用方增加平台分支或重复样式。
- 继续使用系统 `Button`、`.bordered` 和 `.safeAreaInset`；不自绘按钮或监听键盘高度。
- 底栏按钮横向占满可用空间，紧凑高度以终端可视区域为优先。
- 不修改右上角按钮、终端手势、输入状态或网络协议。

## Blockers

- None.
