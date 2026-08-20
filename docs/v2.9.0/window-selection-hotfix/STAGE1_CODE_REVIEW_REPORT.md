# Window 点击切换回归修复代码审查报告

## 结论

- **状态**：✅ Approved
- **范围**：macOS 本地 Window 标签、Mac Remote Viewer Window 标签及对应 E2E
- **审查基线**：`develop/v2.9.0` (`97ea5ff`)

## 根因

拖拽排序把 `selectTerminalWindow` 的查询键改为稳定 ID，但两个标签点击调用点仍传可执行 tmux target
`session:index`。两种标识在新 Host snapshot 中不同，查询必然失败并提前返回。

## 修复审查

- 本地与远程点击均传 `newWindow.stableId`。
- 选择函数参数显式命名为 `stableId`；函数内部解析出当前动态 `target.id` 后才执行本地 tmux 命令或
  远程 Relay 命令。
- 未修改 wire protocol、tmux 重排算法或 split 状态。
- 旧 Host 不提供稳定 ID 时，模型原有 fallback 仍返回 `session:index`，兼容行为不变。

## 验证

- 14 项聚焦测试通过。
- 本地与双 Mac 远程 E2E 均验证点击后真实 tmux active Window 改变。
- macOS Debug 构建及 `codesign --verify --deep --strict` 通过。

## 风险

- 无已知未解决的本次修改风险。
- Tab Reorder 场景后半段仍有既有自动命名 UI 文本断言脆弱性，已与本 hotfix 隔离记录。

