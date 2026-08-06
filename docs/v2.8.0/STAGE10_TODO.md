# Stage 10 TODO：Agent 快捷回复原子提交

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 4/5 tasks
- **Dependencies**: Stage 4 ✅，Stage 9 ✅

## Tasks

- [x] 定位 iOS Send 到 built-in plugin、host 和 tmux 的完整输入链路。
- [x] 将 prompt/reply-after-stop 合并为单次 keystroke delivery。
- [x] 让无 delay 的 mixed-mode keystrokes 使用单次 tmux client 调用。
- [x] 增加并运行 Claude Code、Codex 与 TmuxService 聚焦测试。
- [ ] 完成 macOS host 构建与 iPhone 真机回复验收。

## Decisions

- 不在 iOS 端补发 Enter。viewer 无法可靠判断第一次 Enter 是丢失、延迟还是已经被
  agent 消费，重发会产生重复提交。
- 不引入固定 sleep。问题来自正文和 Enter 被拆成两个独立 host/tmux 调用；延时只会
  降低复现概率，不会消除半提交状态。
- 仅合并不含 `.delay` 的 keystroke 序列。现有 question/menu 导航依赖真实时间间隔，
  继续沿用逐段发送。

## Blockers

- None.

## Verification

- `swift test --filter 'ClaudeCodeKeystrokes|CodexKeystrokes|LocalKeystrokeInput'`：
  40/40 通过。
- 独立 tmux socket 验证 mixed-mode command chain：正文与 Enter 均按序执行。
- `ClaudeSpyServer` macOS Debug 构建、深度签名校验及本机覆盖启动：通过。
- iPhone 顶部回复框连续发送验收：Pending。
