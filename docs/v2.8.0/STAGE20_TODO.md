# Stage 20 TODO：终端传输背压与高吞吐恢复

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 1/8 tasks
- **Dependencies**: Stage 14 ✅, Stage 16 ✅, Stage 17 ✅

## Tasks

- [x] 记录传输指标、背压、重同步和 Relay 原始转发的实施边界。
- [ ] 增加低开销传输指标及聚焦测试。
- [ ] 修复 live dataChunk 切分和公平调度。
- [ ] 实现高水位 snapshot resync。
- [ ] 合并 Mac/iOS terminal feed 并记录耗时。
- [ ] 实现 Relay 加密文本帧原始转发。
- [ ] 修正本机 Relay 稳定源码目录。
- [ ] 完成聚焦测试、完整测试和平台构建验证。

## Decisions

- 不在过载时任意丢弃 terminal bytes；高水位后的唯一降载方式是原子 snapshot resync。
- 不增加二进制协议或压缩；先消除现有 JSON/E2EE 路径中的重复 Relay 编解码。
- 不逐 frame 写日志；指标在内存中聚合并按低频窗口输出，避免观测本身成为瓶颈。
- SwiftTerm feed 保留 MainActor 隔离，只在 runloop 边界合并，不假定其 parser 可后台并发。
- 配置修复只涉及本机 `~/.config/gallager/server/.env.local`，不得提交密钥或机器域名。

## Blockers

- 无。

## Verification

- 待完成。
