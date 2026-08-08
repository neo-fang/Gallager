# Stage 24 TODO：Viewer 断线清理与终端首帧恢复

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 1/7 tasks
- **Dependencies**: Stage 16 ✅, Stage 20 ✅

## Tasks

- [x] 完成连接生命周期、stream 所有权、发送队列和 iOS bootstrap 源码审计。
- [ ] 增加 Viewer 断线按所有权清理、其他 Viewer 保留的失败测试。
- [ ] 增加离线发送丢弃、旧 generation 任务隔离和 send 失败恢复测试。
- [ ] 实现 Viewer presence/Host→Relay 失效时的定向 stream 清理。
- [ ] 实现发送代际、离线门禁、send 失败重连与 bootstrap 慢路径追踪。
- [ ] 实现 iOS 缺失 initial state 的有界 replacement retry。
- [ ] 完成聚焦测试、完整测试、macOS/iOS 构建和代码审查。

## Decisions

- 保留 `ConnectedViewer` 的 MainActor 状态所有权和现有 FIFO 发送语义，以单调 generation
  使旧 Task 失效；不增加第二套发送 actor、锁或 detached task。
- Viewer presence 与 Host→Relay socket 生命周期都可终止当前 stream ownership；清理
  必须按 viewer ID 定向执行，不能调用全局 `stopAllStreams()`。
- bootstrap tracing 只记录慢路径的一条聚合 warning，不把逐帧日志加入 terminal 热路径。
- 本 Stage 不修改 wire protocol、Relay frame 格式、批大小或高水位策略。

## Blockers

- None.

## Verification

- Swift 6.3.3 / Xcode 26.6；Package tools 6.1。
- Xcode App target 使用 MainActor 默认隔离；Package 服务显式以 MainActor/actor 声明所有权。
