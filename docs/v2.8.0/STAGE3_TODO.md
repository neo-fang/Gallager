# Stage 3 TODO：Terminal Payload 缓存性能

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 2/7 tasks
- **Dependencies**: Stage 2 🟡（基于其最新提交开发，不修改 stream recovery 语义）

## 根因证据

- [x] 在真实 Codex pane 上复现 Gallager 持续约 100% CPU。
- [x] `sample` 定位到每个 pipe-pane 数据块都会执行
  `TerminalPayloadCache.extractAndClear`，并遍历约 1000 行 × 187 列的终端缓冲区。
- [ ] 增加可处理跨块边界的 OSC 8 序列检测。
- [ ] 将普通输出路径缩减为只校验已有缓存条目。
- [ ] 增加聚焦单元测试并通过相关测试。
- [ ] 完成 macOS 构建和真实 Codex pane CPU 对比。
- [ ] 人工验证 OSC 8 链接、普通 URL 和终端输入无回归。

## Blockers

- 最终交互验收需要在安装新 Mac App 后完成。
