# Gallager 2.9.0：新 Pane 继承源目录

## 状态

- **状态**：✅ 已完成
- **开发分支**：`feature/pane-split-inherit-cwd`
- **目标分支**：`develop/v2.9.0`

## 问题

Mac 本地与远程界面拆分 Pane 时只传目标 Pane ID。底层 `splitPane` 在未显式传入目录时不向
tmux 提供 `-c`，因此新 Pane 的起始目录不保证与被拆分 Pane 一致。

## 方案

1. 在唯一底层入口 `TmuxService.splitPane` 中，将未显式指定目录映射为 tmux 原生格式
   `#{pane_current_path}`。
2. 显式目录仍具有最高优先级，保持 CLI/API 的现有行为。
3. 使用隔离 tmux socket 验证默认继承与显式覆盖。

## 非目标

- 不修改本地、远程或 iOS 的命令协议。
- 不改变 New Window 或 New Session 的目录语义。
- 不增加目录同步状态或 UI 配置。
