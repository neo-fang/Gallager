# Stage 8 TODO：新建 Window 的 Terminal Stream 竞态修复

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 6/8 tasks
- **Dependencies**: Stage 4 ✅，Stage 5 ✅

## Tasks

- [x] 记录异地 Mac 现场状态并确认故障边界。
- [x] 为 per-pane reader 创建增加 single-flight 所有权。
- [x] 覆盖 reader 创建成功时的并发复用。
- [x] 覆盖 reader 创建失败后的清理与重试。
- [x] 本地 New Window 等待新 pane 出现在缓存后再选中。
- [x] 运行聚焦测试和完整 Swift package 测试。
- [ ] 运行 macOS arm64 Release 构建与签名结构校验。
- [ ] 安装本机验证版并完成真实 New Window 输入回归。

## Evidence

- 异地 Mac 的 Gallager 与 tmux 未崩溃，当前 pane 的 FIFO/`pipe-pane` 状态正常，说明
  故障可自行恢复且不是固定 window 数量上限。
- `PaneStreamManager.subscribe` 与 `updateMonitoring` 都能在 `readers[paneId] == nil`
  时跨 `await` 启动同一 pane reader，目前没有 in-flight 去重。
- 本地 New Window 创建后只查一次 `tmuxService.windows`；并行 refresh 会返回旧缓存，
  该路径未复用其他创建流程已有的 `PaneSurfaceRetry`。
- `PaneLifecycleRaceTests` 5/5 通过；完整 Swift package 回归 1564/1564 通过。

## Decisions

- 修复资源所有权和缓存可见性，不降低 agent/process/pane 轮询频率。
- single-flight 由 `@MainActor` 的 `PaneStreamManager` 持有，不引入锁、detached task 或
  新 actor。
- 不重构整个 tmux refresh 模型；只让需要新 pane 的调用方等待既有重试预算。

## Blockers

- 无。
