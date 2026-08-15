# Stage 9 Code Review Report

## Review Scope

- macOS remote Host drag gesture and header hit testing
- Drag target lifetime and off-screen frame cleanup
- Existing Host ordering and persistence path

## Findings

### P2：Section header 仍截断直接手势

首个修复只把系统数据拖放换成 `DragGesture`，但手柄仍位于 macOS `List` 的 Section header。
真实 App 验收确认顺序仍不变化。算法测试无法覆盖 AppKit List 的事件路由，因此先前的预验收
结论无效。修复必须把交互控件移到普通 List 行，而不是继续更换 header 上的手势 API。

**Resolution**：远程 Host 标题已成为 Section 的第一个普通 List 行，手柄改为高优先级
`DragGesture`；Section header 不再承载任何 Host 排序交互。代码、专项测试、Release 构建、
签名和本机安装均已通过，等待真实交互复验。

The failed `draggable/dropDestination` path has been removed. Host ordering now uses one in-process gesture
and the existing `moveHostPairing` mutation; no secondary order store, Relay message, or iOS behavior was added.
Header frames are removed when views leave the hierarchy, preventing stale off-screen geometry from winning a
later hit test.

## Verification

- Remote Host focused tests: 8/8 passed
- macOS arm64 Release build: passed
- Deep strict App signature verification: passed
- Rejected header-based App source revision: `75dcabb`
- Normal-row App source revision: `e40815d`
- CtrlX CLI: `pong`
- tmux server and pane snapshot: unchanged across installation
- `git diff --check`: passed

## Assessment

Reopened after physical interaction failure. Merge and publication remain blocked until the normal-row
implementation passes physical acceptance.
