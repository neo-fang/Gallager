# Stage 4 TODO：Host Terminal Stream 订阅所有权

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 2/7 tasks
- **Dependencies**: Stage 2/3 🟡（基于最新 stream recovery 与 payload 优化提交开发）

## Tasks

- [x] 通过 loffice 真机确认 relay、E2EE 和 tmux control client 均在线。
- [x] 定位 host 使用无 viewer 身份的整数订阅计数，重连/重试可泄漏计数。
- [ ] 将 command 来源 viewer ID 传入 stream start/stop。
- [ ] 将订阅所有权改为 viewer ID 集合并保持 Start/Stop 幂等。
- [ ] 清理无法读取当前 pane 内容的失效 stream。
- [ ] 增加聚焦测试并运行相关测试与 macOS 构建。
- [ ] 安装 loffice 真机包并用 iOS 验收。

## Decisions

- 不修改网络消息结构。viewer pairId 已存在于 host 的 `ConnectedViewer`，只在 host
  内部回调链保留它。
- 不使用新的重试循环。iOS/Mac viewer 已有一次有限重试；host 只负责让旧状态可被
  确定性清理。
- 不再使用次数计数推测订阅者。订阅所有权必须由稳定的 viewer pairId 表示。

## Blockers

- loffice 真机验收需要安装包含 Stage 4 的新 Mac App，并由 iOS 触发同一 terminal
  页面操作。
