# Stage 16 TODO：远程终端首屏原子揭示

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 0/6 tasks
- **Dependencies**: Stage 14 ✅

## Tasks

- [ ] 为 `PipePaneReader` 增加有序 flush 完成屏障。
- [ ] 在 `TerminalStreamService` 排空 bootstrap batch 后再完成 start command。
- [ ] 将 terminal stream 路由限制到实际 pane 订阅者。
- [ ] 让 Mac viewer 后台装载首屏并在 ready 后一次揭示。
- [ ] 增加顺序、多 Viewer、重连和持续输出聚焦测试。
- [ ] 完成完整测试、macOS Release 构建及双 Mac 验收。

## Decisions

- 保持只有当前可见 pane 维持 Viewer 端实时流，不缓存所有 window 的终端 View。
- 保留约三屏 scrollback，不以牺牲复制和历史浏览换取首屏速度。
- 不增加网络协议消息，使用现有 start command success 作为 bootstrap ready。
- ready 由 FIFO 屏障产生，不使用静默检测或任意毫秒等待。
- 只隐藏 bootstrap 的中间渲染，不丢弃捕获期间的数据。

## Blockers

- None.

## Verification

- Pending.
