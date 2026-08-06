# Stage 4 TODO：Host Terminal Stream 生命周期

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 10/11 tasks
- **Dependencies**: Stage 2/3 🟡（基于最新 stream recovery 与 payload 优化提交开发）

## Tasks

- [x] 通过 loffice 真机确认 relay、E2EE 和 tmux control client 均在线。
- [x] 定位 host 使用无 viewer 身份的整数订阅计数，重连/重试可泄漏计数。
- [x] 确认故障触发条件为 host pane 高吞吐输出，而非普通连接失败。
- [x] 定位实时增量消费早于 initial state，可能占满发送链并触发 Start 超时。
- [x] 将 command 来源 viewer ID 传入 stream start/stop。
- [x] 将订阅所有权改为 viewer ID 集合并保持 Start/Stop 幂等。
- [x] 清理无法读取当前 pane 内容的失效 stream。
- [x] initial state 发送完成前只缓存增量，发送后再按序排空。
- [x] 增加聚焦测试并运行相关测试与 macOS 构建。
- [x] 安装包含 Stage 4 的 loffice 真机包，确认 host 已重连且原 tmux session 保留。
- [ ] 用 iOS 验收 loffice pane 高吞吐输出场景。

## Decisions

- 不修改网络消息结构。viewer pairId 已存在于 host 的 `ConnectedViewer`，只在 host
  内部回调链保留它。
- 不使用新的重试循环。iOS/Mac viewer 已有一次有限重试；host 只负责让旧状态可被
  确定性清理。
- 不再使用次数计数推测订阅者。订阅所有权必须由稳定的 viewer pairId 表示。
- 不丢弃 initial state 期间产生的数据。使用现有 `AsyncStream` 暂存，避免新增第二套
  队列或协议级流控。

## Blockers

- 等待 iOS 在 loffice pane 高吞吐输出期间完成真机验收。
