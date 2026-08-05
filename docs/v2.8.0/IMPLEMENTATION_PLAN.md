# Gallager 2.8.0 实施计划

## 状态

- **状态**：🟡 进行中
- **进度**：1/2 stages

## Stage 1：Tmux Session 重命名

### 目标

为本地和远程 tmux session 提供真正的重命名能力。重命名必须修改 tmux
`session_name`，不能继续用 `@gallager-description` 充当显示别名。

### 实施范围

1. 在 `TmuxService` 增加 `rename-session` 封装，使用精确 target，拒绝空名称、
   当前已存在的名称以及 tmux 不支持的 `:`/`.` 字符。
2. 在共享命令协议增加 `RenameTmuxSession`，由 host 执行并将刷新后的 session
   状态推送给所有 viewer。
3. session 名变化时同步更新 pane stream target 和 control client 索引，避免继续
   使用旧名称捕获终端或管理尺寸。
4. macOS 本地会话和远程会话右键菜单增加 `Rename Session`；macOS 双击会话行
   直接打开相同的名称输入框，单击选择行为保持不变。
5. iOS 会话长按菜单增加 `Rename Session`，复用同一共享编辑 UI 和 relay 命令。
6. 本地重命名后迁移以 session 名为键的选择、文件/浏览器标签、Git 工作区和
   布局保存状态，不把同一 session 当成删除后新建。

### 验收标准

- 本地重命名后，`tmux list-sessions` 只显示新名称，现有 pane 和运行中的进程不变。
- 远程 Mac/iOS viewer 可通过 relay 重命名 host 上的 session，并收到新状态。
- 空名称、非法名称和名称冲突返回明确错误，原 session 保持不变。
- macOS 双击本地或远程会话行弹出预填当前名称的输入框；单击仍只选择会话。
- macOS 和 iOS 右键/长按菜单均包含 `Rename Session`。
- 当前选中的本地会话重命名后仍保持选中，文件、浏览器、Git 和分屏状态不丢失。
- 现有 Description、Emoji、Color、State、Rename Window 和终端输入行为不回归。
- 聚焦单元测试、受影响的 Swift package 测试以及 macOS/iOS 构建通过。

## Stage 2：远程 Terminal Stream 自动恢复

### 目标

当 iPhone 或 Mac viewer 网络切换、App 前后台切换或 relay WebSocket 短暂重连时，
当前终端页自动恢复，不再因为一次瞬时失败永久停留在错误或断开状态。

### 实施范围

1. 让 `LiveTerminalView` 的 stream task 跟随 host 连接状态，而不是只在 view 首次
   出现时执行一次。
2. 连接断开时进入等待恢复状态并取消未发送的输入；连接恢复后重新订阅 terminal
   stream。
3. 对已经请求过的 stream 先发送 `StopTerminalStream` 再重新 `StartTerminalStream`，
   平衡 host 端订阅计数并强制获取完整初始画面；首次订阅不得先 stop，避免影响
   正在观看同一 pane 的其他 viewer。
4. 已连接状态下的瞬时 start 失败自动做一次清理重试；仍失败时显示错误和手动
   `Retry` 操作。
5. 保持多 pane、键盘输入、剪贴板和 terminal stream 消息路由行为不变。
6. Mac viewer 的 `NSViewRepresentable` 在 host 连接状态变化时同步 stream 生命周期；
   `updateNSView` 不重复 start，只有连接边沿或人工 Retry 才触发恢复。
7. iOS 与 Mac 共用同一个 stream recovery 决策模型，不维护两套订阅计数规则。

### 验收标准

- iOS terminal 页面打开后，host 短暂断开再恢复，无需退出页面即可重新显示终端。
- 断线期间不发送键盘输入，恢复后输入顺序正常。
- 首次连接只发送 start；重连和手动重试使用 stop/start，不泄漏 host 订阅计数。
- 同一连接上的一次瞬时 start 失败可自动恢复；持续失败时用户可手动重试。
- 恢复后的 initial state 替换断线前的旧画面，不混入断线期间缺失的数据块。
- 聚焦单元测试、iOS Simulator 构建及 iPhone 真机验证通过。
- Mac viewer 的 host 短断和 start 瞬时失败可在原页面恢复；替换订阅期间旧 stream
  的 `streamEnd` 不得终止新连接。持续失败时保留明确错误和人工 Retry。
- `ClaudeSpyServer` macOS 构建通过，并在本机 viewer 验证远程 terminal 输入无回归。
