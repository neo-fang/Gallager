# Stage 10 TODO：Agent 快捷回复可靠提交

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 6/7 tasks
- **Dependencies**: Stage 4 ✅，Stage 9 ✅

## Tasks

- [x] 定位 iOS Send 到 built-in plugin、host 和 tmux 的完整输入链路。
- [x] 移除 reply composer 的乐观成功反馈与旧反馈恢复。
- [x] 让顶部回复走 host command/response，并在正文与 Enter 间保留 TUI settle。
- [x] 让 built-in prompt/reply-after-stop 使用相同的延迟边界。
- [x] 增加并运行 plugin、TmuxService 与 reply composer 聚焦测试。
- [x] 将 reply 草稿收归 `ResponseState`，真实 working 后清空，失败时继续保留。
- [ ] 完成 macOS host 构建与 iPhone 真机回复验收。

## Decisions

- 不在 iOS 端补发 Enter。viewer 无法可靠判断第一次 Enter 是丢失、延迟还是已经被
  agent 消费，重发会产生重复提交。
- 真机证明单个 tmux client 的无间隔输入仍会丢失提交 Enter，因此撤销该方案。200ms
  是 TUI 文本状态切换的明确输入边界，编码在 keystroke sequence 中并由 host command
  response 覆盖，不是 UI 层盲目 sleep 或重试。
- `Prompt submitted` 不能作为本地状态。reply composer 是否消失只由 agent 的真实
  `working` 状态决定。
- 草稿不能留在固定 `.id` 的 SwiftUI 本地状态；否则 working 后同一 composer id
  重新挂载会复活旧输入。草稿由每轮 `ResponseState` 持有。

## Blockers

- None.

## Verification

- 上一版 mixed-mode 单 client 方案：自动化通过，但 iPhone 真机复测失败，已撤销。
- 修正版聚焦测试：60 tests / 4 suites passed。
- macOS host 与 iOS device 构建：Passed。
- iPhone 顶部回复框连续发送验收：Pending。
