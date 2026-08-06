# Stage 11 TODO：iOS Agent 输入模式

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 0/8 tasks
- **Dependencies**: Stage 9 ✅，Stage 10 ✅

## Tasks

- [ ] 增加默认关闭且可持久化的 Agent 快捷输入设置。
- [ ] 提取并测试普通快捷输入与阻塞表单的展示策略。
- [ ] 默认进入 Agent pane 时启用 terminal 键盘。
- [ ] 保证用户手动切换键盘后不被状态刷新覆盖。
- [ ] 阻塞表单到达时退出 terminal 键盘模式并保持表单可见。
- [ ] 将 Agent pane 的键盘按钮移出 Commands 菜单。
- [ ] 运行聚焦测试和受影响的 Swift package 测试。
- [ ] 完成 iOS device 构建、真机安装与验收。

## Decisions

- 设置只控制非阻塞的 prompt/reply composer。permission、question 和 plan approval
  是完成 Agent 工作流所必需的交互，不能被偏好设置隐藏。
- 默认输入模式只在进入 Agent pane 时初始化；实时 Agent 状态更新不是键盘控制信号。
- 不增加新的 host/relay 配置或协议字段，偏好完全属于 iOS viewer。

## Blockers

- None.

## Verification

- Pending.
