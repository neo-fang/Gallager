# Gallager 2.7 修复实施计划

## 状态

- **状态**：✅ 已完成
- **进度**：3/3 stages

## 问题

iOS 在未配对时默认使用 `wss://relay.gallager.app`，但首次配对页不暴露
`IOSSettings.externalServerURL`。六位配对码不包含 Relay 地址，因此 TestFlight
客户端无法首次连接自托管 Relay。

## Stage 1：首次配对页的 Relay 配置

1. 在 `PairingView` 增加原生 `TextField`，直接绑定现有
   `IOSSettings.externalServerURL`，不增加第二份状态或硬编码私有域名。
2. 配对前验证 URL 必须为 `ws://` 或 `wss://`，及时告知用户而不发出无效请求。
3. 保持六位码自动提交、粘贴、键盘焦点和配对成功后多 host 行为不变。
4. 用聚焦单元测试覆盖 Relay URL 规范化/验证，再构建并安装到真机。

## 验收标准

- 全新安装、未配对状态下可见并修改 Server URL。
- 值直接持久化到 `IOSSettings`，重启 App 后不丢失。
- 仅接受 `ws`/`wss` Relay URL，配对 API 仍通过现有 `httpURL` 转换访问。
- 真机能通过自托管 Relay 配对，并看到默认 tmux socket 中的 session。
- 未配置 APNs 时不影响前台 WebSocket 与终端功能，不宣称后台推送通过。

## Stage 2：macOS 输入法事件修复

1. 让内层 SwiftTerm `TerminalView` 成为真正的 first responder，使普通字符、
   marked text 和候选提交都走其原生 `NSTextInputClient` 实现。
2. 外层 `InteractiveTerminalView` 继续负责滚动、链接、复制、文件拖放和焦点边框，
   不重复实现一套易出错的 IME 协议转发。
3. 保留 Shift+Enter、Cmd+C/Cmd+V、多 pane 焦点与编辑器关闭后恢复焦点的行为。
4. 增加聚焦单测并在独立 tmux session 验收英文、数字、中文、空格和回车。

## Stage 2 验收标准

- SwiftTerm 内层视图是窗口的 first responder。
- 普通英文/数字按键与中文输入法候选提交都能进入目标 tmux pane。
- 切换 pane 时输入不会发往兄弟 pane。
- iOS Relay 输入链路不受影响。

## Stage 3：macOS 既有 tmux pane 的 Codex 终端识别

1. 在 tmux control client 启动时规范化缺失、空值或 `dumb` 的 `TERM`，避免
   Codex 将无 TTY 的控制连接误判为用户终端。
2. 保留已经有效的 `TERM`，不修改用户 tmux 配置、pane 环境或 shell 启动文件。
3. 增加环境规范化单测，并在 `zen_coding` 的既有 pane 中验收 Codex TUI。

## Stage 3 验收标准

- Finder/launchd 环境下创建的 control client 向 tmux 声明 `xterm-256color`。
- 从真实终端启动时保留其非空、非 `dumb` 的 `TERM`。
- Gallager 新旧 pane 中启动 Codex 均不再出现 `TERM=dumb` 告警。
- iOS、Relay、tmux pane 的实际环境与用户配置不受影响。
