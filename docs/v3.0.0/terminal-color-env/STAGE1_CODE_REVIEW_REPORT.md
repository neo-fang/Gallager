# 终端禁色环境隔离修复代码审查报告

## 结论

- **状态**：✅ Approved
- **范围**：tmux Session、Window、Pane 的 shell 创建边界及真实 tmux 回归测试
- **审查结果**：无 P1、P2 或 P3 问题

## 根因

CtrlX 从 Agent shell 启动时可能继承 `NO_COLOR=1`。新 tmux server 会把它复制到
global environment，再复制到 session environment；Codex 等 TUI 检测到变量存在后
主动禁用 ANSI 配色。仅设置 `COLORTERM=truecolor` 无法抵消该语义。

## 修复审查

- 新 session 创建前清除 tmux global environment 中继承的 `NO_COLOR`。
- 新 window 和 pane 创建前清除目标 session environment 中旧版本留下的
  `NO_COLOR`，升级后不要求重启已有 tmux session。
- 清理和 shell 创建放在同一个 tmux 命令序列中，不留下异步竞态窗口。
- 不设置 `FORCE_COLOR`，不修改用户 dotfiles；用户仍可在 shell 启动文件中主动导出
  `NO_COLOR`。
- 现有 `TERM`、`COLORTERM`、truecolor、OSC 10/11 及 window/pane 行为未改变。

## 验证

- `git diff --check` 通过。
- `TmuxTerminalEnvironmentTests` 3/3 通过。
- `TmuxPaneSplitTests` 与 `WindowReorderTests` 6/6 通过。
- macOS Debug App 构建通过。
- SwiftLint 未安装；Xcode 构建脚本仅报告该工具缺失，Swift 编译无错误。

## 剩余风险

- 已经运行中的 shell 或 TUI 不会被强制改写环境；需要新建 shell，或退出并重启该
  TUI，才能看到恢复后的配色。这避免了向用户现有进程注入环境变量。
