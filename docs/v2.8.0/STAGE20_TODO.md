# Stage 20 TODO：终端传输背压与高吞吐恢复

## Stage Status

- **Status**: 🟡 In Progress（待打包后真机持续输出验收）
- **Progress**: 8/8 tasks
- **Dependencies**: Stage 14 ✅, Stage 16 ✅, Stage 17 ✅

## Tasks

- [x] 记录传输指标、背压、重同步和 Relay 原始转发的实施边界。
- [x] 增加低开销传输指标及聚焦测试。
- [x] 修复 live dataChunk 切分和公平调度。
- [x] 实现高水位 snapshot resync。
- [x] 合并 Mac/iOS terminal feed 并记录耗时。
- [x] 实现 Relay 加密 frame 原始转发。
- [x] 修正本机 Relay 稳定源码目录。
- [x] 完成聚焦测试、完整测试和平台构建验证。

## Decisions

- 不在过载时任意丢弃 terminal bytes；高水位后的唯一降载方式是原子 snapshot resync。
- 不增加二进制协议或压缩；先消除现有 JSON/E2EE 路径中的重复 Relay 编解码。
- 不逐 frame 写日志；指标在内存中聚合并按低频窗口输出，避免观测本身成为瓶颈。
- SwiftTerm feed 保留 MainActor 隔离，只在 runloop 边界合并，不假定其 parser 可后台并发。
- 配置修复只涉及本机 `~/.config/gallager/server/.env.local`，不得提交密钥或机器域名。

## Blockers

- 无。

## Verification

- `swift test --quiet`：1639 tests / 232 suites 全部通过（45.638s）。
- 65,536-byte live output 聚焦测试确认拆为 8 个 8,192-byte `dataChunk`。
- macOS `ClaudeSpyServer` Release（arm64、ad-hoc 签名）构建通过。
- iOS `ClaudeSpy` Debug generic-device 构建通过。
- Swift 6.3 Jammy/Linux Release Relay 构建通过；容器 `/health` 返回 `{"status":"ok"}`。
- Relay 原始 frame、非法密文、snapshot wire round-trip、队列高水位和 feed 合并测试通过。
- `~/.config/gallager/server/.env.local` 中 `GALLAGER_SOURCE_DIR` 已指向主仓库；文件权限保持 `600`。
- `git diff --check` 通过。
- 待使用后续 DMG/真机产物做两分钟持续输出验收，并观察 debug 窗口指标确认真实网络下无长尾回放。
