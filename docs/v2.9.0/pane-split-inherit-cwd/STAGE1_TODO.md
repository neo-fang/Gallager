# 新 Pane 继承源目录 TODO

## Stage Status

- **Status**：✅ Completed
- **Progress**：4/4 tasks
- **Dependencies**：无

## Tasks

- [x] 确认本地、远程与 iOS 拆分 Pane 共用底层入口
- [x] 实现默认继承源 Pane 当前目录
- [x] 增加真实 tmux 回归测试
- [x] 运行聚焦测试与 macOS 构建

## Blockers

- 当前无阻塞。

## Verification

- 隔离 tmux 实测：源 Pane 与默认新 Pane 的 `pane_current_path` 一致，显式目录正确覆盖。
- `TmuxPaneSplitTests`：1 个测试、1 个套件通过。
- macOS Debug 工程完整构建通过；仅有既有的 SwiftLint 未安装警告。
- `swiftc -frontend -parse` 与 `git diff --check` 通过。
