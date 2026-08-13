# Gallager 2.9.0：macOS Window 拖拽排序修复

## 状态

- **状态**：✅ 实施完成，待应用内验收
- **开发分支**：`feature/window-drag-reorder-fix`
- **目标分支**：`develop/v2.9.0`

## 1. 问题

macOS 本地和远程 Window 标签拖拽存在三个相互叠加的缺陷：

1. 标签只显示前置落点指示线，但向右拖动会插到目标之后，落点与结果不一致。
2. UI 使用会随排序变化的 `session:index` 保存窗口身份；tmux 重编号后，旧 ID 会指向其他窗口。
3. tmux 重排硬编码从索引 `0` 开始，并以 `move-window -k` 覆盖目标；这会破坏启用
   `base-index 1`、`renumber-windows on` 或稀疏索引的会话。

连续拖拽还会并发执行重排，失败时 UI 不回滚，进一步放大错序。

## 2. 目标

- 每次拖拽都把标签准确插到指示线所在的目标之前；末尾落点明确移动到末尾。
- 通过 tmux `#{window_id}`（如 `@3`）追踪逻辑窗口，排序和重编号后身份不变。
- 保留会话当前索引集合，不假设起始索引为 0，不覆盖或删除窗口。
- 同一会话一次只允许一个 Window 重排；失败时恢复标签顺序和选择状态。
- 本地 Host 与远程 Mac Viewer 使用同一语义。

## 3. 设计

### 3.1 稳定身份

在 pane 元数据末尾增加可选 `tmuxWindowId`，并提供 `stableWindowId` 回退：

```text
tmux #{window_id} -> PaneInfo -> PaneState -> LocalTmuxWindow/TmuxWindow
```

`LocalTmuxWindow.id` / `TmuxWindow.id` 继续表示可直接发给 tmux 的 `session:index`；仅标签顺序、
split 归属和拖拽 payload 使用稳定 ID。旧 Host 未发送新字段时回退到现有 ID，保持协议兼容。

### 3.2 tmux 重排

重排前重新查询该 session 的 `window_id + window_index`，要求请求 ID 与实时窗口集合完全一致。
以现有索引的升序序列作为目标槽位，通过 `swap-window -s @id -t =session:index` 逐项交换。
每次交换同步更新内存位置表，最后只刷新一次 pane 状态。

该算法不需要临时高位索引、不使用 `-k`，并自然支持 `base-index 1`、稀疏索引和自动重编号。

### 3.3 UI 事务

- 统一纯函数负责“移动到目标之前”和“移动到分区末尾”。
- Window 子序列变化时保存旧的完整 `tabOrder`，设置会话级 pending 标记并乐观更新 UI。
- pending 期间拒绝新的 Window 重排。
- Host 成功后保留新顺序，并按稳定 ID 恢复选择；失败时恢复旧顺序和选择并展示错误。
- 文件和浏览器标签继续沿用同步本地排序，不被 Window pending 阻塞。

## 4. 非目标

- 不改变 tmux Window 名称、pane 布局或 session 排序。
- 不增加 Relay 服务端状态；稳定 ID 只随现有加密 session state 传输。
- 不支持跨 session 拖动 Window。
- 不重构整个 tab/split 状态模型。

## 5. 验收

- A→C、C→A、拖到末尾均与指示线一致。
- `base-index 0/1`、`renumber-windows on/off` 下逻辑窗口顺序正确且无窗口丢失。
- 排序后选中 Window、右侧 split Window、混排文件/浏览器标签仍指向原逻辑对象。
- 快速连续拖拽不会并发执行；tmux 命令失败会回滚。
- 本地和远程 Mac Viewer 行为一致，相关单元测试与 macOS 构建通过。
