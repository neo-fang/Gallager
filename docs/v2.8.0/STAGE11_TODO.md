# Stage 11 TODO：iOS Agent 输入模式

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 7/8 tasks
- **Dependencies**: Stage 9 ✅，Stage 10 ✅

## Tasks

- [x] 增加默认关闭且可持久化的 Agent 快捷输入设置。
- [x] 提取并测试普通快捷输入与阻塞表单的展示策略。
- [x] 默认进入 Agent pane 时启用 terminal 键盘。
- [x] 保证用户手动切换键盘后不被状态刷新覆盖。
- [x] 阻塞表单到达时退出 terminal 键盘模式并保持表单可见。
- [x] 将 Agent pane 的键盘按钮移出 Commands 菜单。
- [x] 运行聚焦测试和受影响的 Swift package 测试。
- [ ] 完成 iOS device 构建、真机安装与验收。

## Decisions

- 设置只控制非阻塞的 prompt/reply composer。permission、question 和 plan approval
  是完成 Agent 工作流所必需的交互，不能被偏好设置隐藏。
- 默认输入模式只在进入 Agent pane 时初始化；实时 Agent 状态更新不是键盘控制信号。
- 不增加新的 host/relay 配置或协议字段，偏好完全属于 iOS viewer。

## Blockers

- None.

## Verification

- Agent 输入展示策略：3 tests / 1 suite passed。
- `ClaudeSpyFeatureTests`：45 tests / 5 suites passed。
- iOS device 无签名构建：Passed。
- 使用本机 Apple Development 证书签名、校验并安装到 ZengJice iPhone：Passed。
- 默认模式、设置切换、直接键盘按钮和阻塞表单真机交互：待用户验收。
