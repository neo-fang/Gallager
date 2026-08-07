# Stage 17 TODO：Mac Viewer 交互延迟与主线程公平性

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 0/7 tasks
- **Dependencies**: Stage 14 ✅, Stage 16 ✅

## Tasks

- [ ] 建立同一远程 pane 的端到端输入与回显测量边界。
- [ ] 对比 Mac/iOS Viewer 热路径并记录平台差异。
- [ ] 验证本地 sessions 空闲及持续输出对 MainActor 的影响。
- [ ] 实施最小的交互发送与/或终端 feed 公平性修复。
- [ ] 增加输入顺序、调度上界、取消和持续输出回归测试。
- [ ] 完成完整测试、macOS Release 构建、签名及 DMG 校验。
- [ ] 完成同一网络下 Mac/iOS 与双 Mac 真机验收。

## Decisions

- 不使用本地字符预回显，屏幕内容只来自 Host PTY stream。
- 共用网络路径已由静态代码确认；平台差异必须从调度或渲染证据中定位。
- 可以用更多但有界的 CPU 换取交互延迟，不接受无界 Task、队列或 tmux 子进程增长。
- 保持 E2EE、relay 协议和 terminal 字节顺序不变。

## Blockers

- 需要 Release 候选版在同一远程 pane 上进行最终主观体验对照。

## Verification

- Pending.
