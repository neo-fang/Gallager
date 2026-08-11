# Stage 4 TODO：macOS 本地终端输入延迟

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 1/8 tasks
- **Dependencies**: v2.8.0 Stage 18、26、31 ✅

## Tasks

- [x] 采样并定位 MainActor、Observation、tmux control 与 terminal feed 热点。
- [ ] 抑制无变化的 tmux/pane 状态发布，并为发布语义增加聚焦测试。
- [ ] 将 Viewer session-state 构造改为不扰动本地 UI 的纯快照路径。
- [ ] 增加本地按键到回显/feed 的低开销端到端指标。
- [ ] 为待处理本地输入增加公平的 terminal feed 调度。
- [ ] 降低 URL 装饰、无限动画和重复扫描造成的主线程放大。
- [ ] 完成聚焦测试、构建和相同现场的前后性能采样。
- [ ] 完成代码审查、更新文档并合入 `develop/v2.9.0`。

## Decisions

- 不改变 tmux 输入语义，不实现 speculative local echo。
- 保留单一有序按键 FIFO 与同一 runloop 内的 Meta 合并。
- 优先删除无效状态写入；不以缓存或新 ViewModel 掩盖 Observation fan-out。
- Viewer 快照允许显式读取 tmux，但不得顺带写入本地 UI model。
- 指标为进程内聚合；只在超过阈值时记录摘要，不逐键同步写日志。

## Baseline

- Agent TUI 可见时 Gallager 约 80%–105% CPU；普通 zsh 多数为 12%–26%。
- 3352 个主线程样本中，`NSRunLoop.flushObservers` 2089、
  `GraphHost.flushTransactions` 1496、`AG::Subgraph::update` 1364。
- 现有持久 tmux control-mode 200 次提交约 0.01 秒；该结果不包含 MainActor 排队和回显。
- 5 秒 pane validation、10 秒 discovery/reconciliation 和 Viewer snapshot 都会触发刷新。

## Blockers

- None.

## Verification

- Pending.
