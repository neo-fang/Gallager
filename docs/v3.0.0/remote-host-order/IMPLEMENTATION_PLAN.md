# CtrlX 3.0.0：Viewer 远程 Host 顺序

## 问题

macOS 与 iOS Viewer 当前按 `pairedHosts` 的配对写入顺序展示远程 Host。已有 session 顺序
可以调整，但 Host section 本身不能拖拽，用户无法把高频机器固定在列表前面。

## 设计

1. 继续以 Viewer 本地持久化的 `pairedHosts` 数组作为唯一 Host 顺序来源，不增加第二份顺序配置。
2. 新配对追加到末尾；已有配对更新时原位替换，不得被移动到末尾。
3. 拖动 Host 到目标 Host 时，向上拖放到目标之前，向下拖放到目标之后，覆盖首尾位置。
4. macOS sidebar 的远程 Host header 提供拖拽手柄；iOS 在 Manage Hosts 的 `Edit` 模式下使用系统 List 重排。
5. iOS Sessions 页的 `Edit` 只负责 session 行重排；Host 与 session 的排序入口明确分离。
6. 顺序变化只影响当前 Viewer，不修改 Relay 协议、远端 tmux 或其他 Viewer。

## 实施范围

- 在 `ClaudeSpyCommon` 增加小型纯函数，统一 Host ID 的拖放移动语义并覆盖边界测试。
- 在 macOS `AppSettings` 与 iOS `IOSSettings` 中原子更新并持久化 `pairedHosts`。
- macOS Host header 使用拖放载荷校验；iOS Manage Hosts 使用系统 `onMove` 坐标。
- 验证 sidebar、iOS Sessions、Manage Hosts、菜单栏与快捷键入口都读取同一顺序。

## 非目标

- 不跨 Viewer、Relay 或 iCloud 同步顺序。
- 不改变远端 Host 的 tmux session/window 顺序。
- 不允许 Host 与 session 混合或跨层级拖放。
- 不新增单独的排序设置页或排序模式。

## 验收标准

- macOS 与 iOS 均可把任意 Host 移到首位、中间或末位。
- iOS Host 拖拽与现有 session 拖拽互不冲突。
- App 重启、Host 断线重连、配对信息更新后顺序保持。
- 新 Host 稳定追加；删除 Host 不影响其余顺序。
- 共享算法测试、设置持久化测试及 macOS/iOS 构建通过。
