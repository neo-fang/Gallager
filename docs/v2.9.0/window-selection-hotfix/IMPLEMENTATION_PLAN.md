# Gallager 2.9.0：macOS Window 点击切换回归修复

## 状态

- **状态**：✅ 已完成并通过本地与远程验收
- **开发分支**：`hotfix/window-selection-regression`
- **目标分支**：`develop/v2.9.0`

## 问题

Window 拖拽排序改用 tmux 稳定 Window ID（`@N`）后，Mac 本地和远程标签点击仍把可变的
`session:index` 传给只接受稳定 ID 的选择函数。查找失败后函数直接返回，导致标签无法切换。

## 修复

1. 本地和远程标签点击统一传递 `stableId`。
2. 将选择函数参数命名改为 `stableId`，让调用契约在编译点可见。
3. 在本地与远程 Tab Reorder E2E 中增加真实 tmux active Window 断言，避免只依赖 UI 高亮。
4. 构建、覆盖安装并验证本地 Window 点击；远程路径使用双 Mac E2E 或等价链路验证。

## 非目标

- 不改变拖拽排序、split 或 tmux Window ID 协议。
- 不增加新的状态层或兼容分支。
