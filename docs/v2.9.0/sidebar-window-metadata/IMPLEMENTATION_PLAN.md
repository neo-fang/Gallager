# Gallager 2.9.0 实施计划：Sidebar 活跃 Window 元数据

## 状态

- **状态**：🟡 实施中
- **开发分支**：`feature/sidebar-active-window-fields`
- **前置依赖**：`develop/v2.9.0` @ `8c7120f` ✅

## 1. 问题

macOS Sidebar 每行代表一个 tmux session，但普通终端行的 `Terminal Title` 当前从整个
session 中取第一个非空 pane title。它可能来自非活跃 window，而同一行的 command、path
和 git branch 却来自活跃 pane，导致一行混合多个 window 的信息。

`Terminal Title` 由终端程序通过 OSC 0/2 设置，不等于 tmux `window_name`。现有 Sidebar
字段没有 `window_name`，用户无法选择显示活跃 tmux window 的名称。本地 Host、远端 Mac
Viewer 和排序逻辑对 title 的取值也不完全一致。

## 2. 目标

1. 保持 Sidebar 一行代表一个 tmux session。
2. 新增可配置字段 `Tmux Window Name`，显示活跃 window 的 `windowName`。
3. `Terminal Title` 保持 OSC title 语义，但统一只读取活跃 pane。
4. 展示与 alphabetical primary-label 排序使用完全相同的活跃 window/pane 元数据。
5. 不在 session 行拼接所有 window name；window 枚举继续由顶部标签承担。

## 3. 设计

### 3.1 字段模型

在共享 `SidebarField` 增加稳定 Codable case `windowName`，显示名为
`Tmux Window Name`。该字段同时适用于 agent 与普通终端布局，但不改变现有用户配置和
默认字段顺序；用户可在 Settings → Sidebar 主动启用。

### 3.2 唯一数据选择规则

每个 session 先解析一次活跃 window 和活跃 pane：

```text
session.activeWindow
  -> activePane
     -> windowName / terminalTitle / command / path / gitBranch
```

本地 Host、远端 Mac Viewer 和菜单排序均使用该规则。不得再扫描 session 中第一个非空
terminal title，也不得回退到其他非活跃 window 的 title。

### 3.3 非目标

- 不把 Sidebar 改成 session/window 两级树。
- 不列出或拼接一个 session 的所有 window name。
- 不改变顶部 window tabs、tmux rename 或 Relay 协议。
- 不把 OSC title 重命名为 window name，也不从 title 猜测 tmux window。
- 不迁移或覆盖用户已保存的 Sidebar 字段配置。

## 4. 验证

- 纯模型测试覆盖 `windowName` 的 primary-label 解析。
- 本地多 window 测试证明 title/name 均来自 active window，而非数组中的第一个非空值。
- 远端多 window 测试证明展示与排序选择相同。
- Sidebar 设置测试覆盖新字段可选且默认布局不被隐式改写。
- 运行相关 Swift tests、macOS Debug 构建和 `git diff --check`。
