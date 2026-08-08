# Stage 27 TODO：iOS 窗口切换导航稳定性

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 5/6 tasks
- **Dependencies**: Stage 26 ✅

## Tasks

- [x] 定位窗口切换时自动返回 session 列表的导航触发路径。
- [x] 提取并测试窗口选择/缺失状态决策。
- [x] 将瞬时空 session 改为可取消的延迟确认。
- [x] 让 App 内主动关闭 session 成功后显式返回。
- [x] 完成聚焦测试、完整测试和 iOS device build。
- [ ] 安装真机验收，合入主仓库并清理 worktree。

## Root cause

`WindowLayoutView` 的窗口 ID 观察器在当前选择消失且 `sessionWindows` 瞬时为空时立即
调用 `dismiss()`。新建/切换窗口会触发密集完整状态刷新，因此一次暂时不完整的快照就会
被误判为 session 已结束；表现为终端页面“闪退”回列表，而非 iOS 进程崩溃。

## Blockers

- None.

## Verification

- `WindowSelectionReconciliationTests`：6/6 通过。
- Swift Package：1658 项测试、236 个测试集全部通过。
- iOS generic device Debug build：通过（`CODE_SIGNING_ALLOWED=NO`）。
- `git diff --check`：通过。
