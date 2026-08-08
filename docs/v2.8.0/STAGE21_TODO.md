# Stage 21 TODO：iOS 底部输入按钮栏紧凑化

## Stage Status

- **Status**: ✅ Completed
- **Progress**: 6/6 tasks
- **Dependencies**: Stage 15 ✅

## Tasks

- [x] 定位共享底部按钮栏及单 pane/多 pane 调用边界。
- [x] 将可见按钮栏高度从约 56pt 压缩到约 28pt。
- [x] 保留键盘显隐、禁用状态、设置切换与无障碍语义。
- [x] 完成代码检查和 iOS 构建验证。
- [x] 自适应回收部分 Home Indicator 空白且不影响键盘和无 Home Indicator 设备。
- [x] 完成 iPhone 真机视觉与点击验收。

## Decisions

- 只修改 `TerminalKeyboardBar`，不在调用方增加平台分支或重复样式。
- 继续使用系统 `Button`、`.bordered` 和 `.safeAreaInset`；不自绘按钮或监听键盘高度。
- 底栏按钮横向占满可用空间，紧凑高度以终端可视区域为优先。
- 只回收实际 bottom safe-area inset 的一半且最多 16pt；键盘显示时回收值为 0。
- 不修改右上角按钮、终端手势、输入状态或网络协议。

## Blockers

- None.

## Verification

- 共享组件从 `.callout`、44pt 内容高度和 6pt 垂直边距改为 `.caption`、18pt 内容
  高度、`.mini` 系统控件及 2pt 垂直边距；两处调用无需修改。
- iPhoneOS arm64 无签名构建通过，确认 `TerminalKeyboardBar.swift` 进入 iOS 编译目标。
- `git diff --check` 通过；本次只改 SwiftUI 样式参数，没有新增可单元测试的状态逻辑。
- 使用个人开发团队的既有 profile 完成真机签名构建；付费能力仅通过构建期空 entitlement
  排除，不修改仓库默认 Push Notifications/App Groups 配置。
- 已覆盖安装并启动到 `ZengJice iPhone`，Bundle ID 保持
  `com.zengjice.gallager.local`；等待真机视觉与点击验收。
- 单 pane 与多 pane 父视图均在插入底栏前读取真实 bottom safe-area inset；底栏在
  键盘隐藏时回收其一半、上限 16pt，键盘显示时回收 0。
- 自适应安全区版本通过 iPhoneOS 真机签名构建、深度签名校验，并再次覆盖安装和启动；
  真机确认按钮栏高度、底部留白、按钮点击及 Home Indicator 手势均符合预期。
