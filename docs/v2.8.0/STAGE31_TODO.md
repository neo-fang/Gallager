# Stage 31 TODO：iOS Terminal Stream 租约与抗抖恢复

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 1/7 tasks
- **Dependencies**: Stage 30 ✅

## Tasks

- [x] 审计 `Terminal Stream Ended`、Viewer 重连和 Host stream ownership 的完整链路。
- [ ] 为 Viewer Relay 生命周期补齐 connection generation、精确 socket 失效和发送失败重连。
- [ ] 增加 terminal stream lease 与向后兼容的 Host ownership 语义。
- [ ] 增加 iOS handler registration ownership 和有界稳定恢复策略。
- [ ] 调整多 Host keepalive 抗抖并增加异常 stop reason 诊断。
- [ ] 完成聚焦测试、完整 Swift package 与 macOS/iOS 构建。
- [ ] 完成代码审查、更新文档并合入 `develop/v2.8.0`。

## Decisions

- lease 是每次 Start 的客户端 UUID，不是引用计数。Host 只接受当前 lease 的 Stop。
- 协议字段保持 optional；旧 Viewer/Host 按原有无 lease 语义工作，避免静默破坏异地旧版本。
- recovery 使用短时间失败上限与稳定期复位，不使用无限重试或永久的一次性额度。
- keepalive 保留现有应用层 ping/pong，只把单次漏报改为连续两轮确认，不引入第二套网络栈。

## Blockers

- None.

## Verification

- Pending.
