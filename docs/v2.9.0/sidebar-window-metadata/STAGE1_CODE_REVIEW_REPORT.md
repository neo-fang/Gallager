# Stage 1 Code Review：Sidebar 活跃 Window 元数据

## 结论

- **状态**：✅ Approved
- **范围**：Sidebar 字段模型、本地/远端 session 行、排序标签、设置预览与聚焦测试。
- **阻断问题**：无。

## 审查结果

### P0 / P1 / P2 / P3

- 未发现。

### 已核对的风险

1. Sidebar 仍保持一行一个 tmux session，没有引入 window 树或重复顶部 tabs。
2. `Tmux Window Name` 是独立可选字段，不改变默认字段和已有自定义布局。
3. `Terminal Title`、command、path、git branch 只取 active window 的 active pane，
   不再混入非活跃 window 的首个非空 title。
4. 本地、远端展示与 alphabetical primary-label 排序共用同一元数据选择规则。
5. 新字段只扩展本地偏好枚举，不修改 Relay 消息或持久化协议。

## 验证证据

- 聚焦 Swift tests：12 个测试、2 个 suite 全部通过。
- macOS Debug app build：通过。
- whitespace 检查：通过。

## 剩余验收

- 在真实多-window tmux session 中切换活跃 window，确认 Sidebar 按配置即时展示对应
  `Tmux Window Name` 与 active pane 的 `Terminal Title`。
