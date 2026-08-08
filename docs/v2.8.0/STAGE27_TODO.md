# Stage 27 TODO：iOS 窗口切换导航稳定性

## Stage Status

- **Status**: ✅ Completed
- **Progress**: 7/7 tasks
- **Dependencies**: Stage 26 ✅

## Tasks

- [x] 定位窗口切换时自动返回 session 列表的导航触发路径。
- [x] 提取并测试窗口选择/缺失状态决策。
- [x] 删除空 session 快照触发的隐式导航和固定延时。
- [x] 删除 pane `streamEnd` 对父导航的控制并增加一次有界恢复。
- [x] 让 App 内主动关闭 session 成功后显式返回。
- [x] 重新完成聚焦测试、完整测试和 iOS device build。
- [x] 重新安装真机验收，合入主仓库并清理 worktree。

## Root cause

真机确认 App 进程没有退出，问题是导航 pop。存在两条错误的隐式导航路径：

1. `WindowLayoutView` 根据空 session 快照推断 session 已关闭；第一版将其改为 2 秒确认，
   但快照延迟没有时间上界，该推断仍不可靠。
2. 每个 pane 的 `LiveTerminalView` 收到 `streamEnd` 都调用环境 `dismiss()`。新建/切换窗口
   会停止旧流，延迟到达的结束帧因此可能把整个父 session 页面弹出。

## Blockers

- None.

## Verification

- `WindowSelectionReconciliationTests`：6/6 通过。
- 第一版 Swift Package：1658 项测试、236 个测试集全部通过，但真机验收失败。
- 第一版 iOS generic device Debug build：通过（`CODE_SIGNING_ALLOWED=NO`）。
- 真机问题发生后 Gallager 进程仍存活，排除进程 crash。
- 修正版聚焦测试：10/10 通过。
- 修正版 Swift Package：1659 项测试、236 个测试集全部通过。
- 修正版 iOS generic device Debug build：通过（`CODE_SIGNING_ALLOWED=NO`）。
- `git diff --check`：通过。
- 真机验收通过：新建和切换 window 后不再异常返回 session 列表；验收版本为
  `2.7 (40) · 20260808-172822 · 8034150df0cf`。
