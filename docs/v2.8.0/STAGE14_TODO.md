# Stage 14 TODO：远程终端输入与回显延迟

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 0/6 tasks
- **Dependencies**: Stage 4 ✅, Stage 7 ✅

## Tasks

- [ ] 建立连续输入、连续输出和顺序保持的聚焦回归测试。
- [ ] 将 viewer keystroke 批处理改为 10ms 有界窗口。
- [ ] 将 host terminal stream 改为 16ms 固定节拍 flush。
- [ ] 构建 Release 并在相同高频输出 pane 重新采样。
- [ ] 依据 Release 热点最小化 pipe-pane/MainActor 数据路径开销。
- [ ] 完成全量测试、Release DMG、覆盖安装和真实远程输入验收。

## Decisions

- 不做 terminal 本地回显；host PTY 始终是唯一显示真相。
- 不删除发送串行化；只去掉会被连续事件无限重置的尾随等待。
- 输出 flush 采用固定节拍，不使用自适应算法或新协议字段。
- 并发优化保持 `PipePaneReader` actor 拥有解析状态，MainActor 只接收有序结果。
- Release 测量与 Debug 定位严格分开。

## Baseline Evidence

- Mac 和 iOS viewer 控制同一本机 host 均出现输入回显延迟，问题位于共用链路。
- `KeystrokeDebouncer` 为 30ms 尾随 debounce，`TerminalStreamService` 为 50ms
  尾随 debounce，单字符已有约 80ms 人为等待。
- 当前已安装 Debug App 在 Codex 高频输出时约占用一个 CPU core；采样显示
  `PipePaneReader.processIncomingData` 多轮过滤和 MainActor 数据路径活跃。
- relay/nginx 运行正常且无明显发送队列积压，暂不修改 relay。

## Blockers

- None.

## Verification

- Pending.
