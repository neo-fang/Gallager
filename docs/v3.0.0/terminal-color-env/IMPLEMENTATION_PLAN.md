# CtrlX 3.0.0：终端禁色环境隔离修复

## 问题

当最后一个 tmux session 退出后，CtrlX 会重新启动 tmux server。如果 CtrlX App
恰好由带有 `NO_COLOR` 的 shell 或 Agent 环境启动，tmux global environment 会继承
该变量，之后启动的 Codex 等 TUI 会主动禁用全部 ANSI 颜色。`COLORTERM=truecolor`
无法覆盖 `NO_COLOR`。

## 实施范围

1. CtrlX 创建 tmux session 前，从 tmux global environment 清除 `NO_COLOR`。
2. 不引入 `FORCE_COLOR`，不修改用户 shell 配置，也不影响已有 tmux session。
3. 增加回归测试，验证 App 启动环境包含 `NO_COLOR=1` 时，新 session 不会继承它。
4. 保持现有 `TERM`、`COLORTERM`、truecolor 和 OSC 10/11 逻辑不变。

## 验收标准

- 新 tmux server 不向 pane 传递 `NO_COLOR`。
- 既有 tmux server 的 global environment 含 `NO_COLOR` 时，新 session 也会清除它。
- 终端环境、TmuxService 相关测试及 macOS 构建通过。
- 本机 CtrlX 覆盖安装后签名有效，已有 tmux session 不被关闭。
