# Stage 28 Code Review Report

## Scope

- Host 端 tmux literal 输入的参数构造。
- iOS 粘贴文本经过空格分段后的进程发送路径。
- 独立 tmux socket 的真实输入回归测试。

## Findings

### Critical / High / Medium

- **High（已修复）**：`tmux send-keys -l` 后未提供选项终止符。用户文本批次以 `-` 开头时，
  tmux 将其解析为自身选项并中止后续输入，造成粘贴内容部分丢失。

### Low

- None.

## Correctness checks

- literal 文本统一生成为 `send-keys -l -- <text>`；普通文本和连字符参数走同一路径。
- 不修改 `TmuxKey` 分段、Relay、E2EE、消息协议或 iOS 剪贴板状态。
- 进程参数测试覆盖 `--set`、`-n`、空格分段和普通文本。
- 独立 tmux socket 测试确认完整命令实际进入 pane，而非只检查 mock 参数。
- 14 项聚焦测试、1661 项完整测试、macOS 构建及严格签名校验通过。

## Assessment

Approved. 修复集中在唯一 literal 入口，只增加 tmux 标准选项终止符，没有新增状态、协议分支或
平台专用粘贴实现。旧 iOS Viewer 与更新后的 Host 保持兼容。
