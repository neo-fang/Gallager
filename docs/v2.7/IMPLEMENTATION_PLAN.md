# Gallager 2.7 修复实施计划

## 状态

- **状态**：✅ 已完成
- **进度**：7/7 stages

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

## Stage 4：macOS 远程图片粘贴大小适配

1. 保留 Relay 的 1 MiB WebSocket frame 上限和 `SendDroppedFiles` 协议，避免为
   图片粘贴扩大 Relay 单连接内存风险或引入分片状态机。
2. 发送前将 TIFF 剪贴板图片规范化为 PNG；若编码后仍超过 Relay 原始数据预算，
   则转换为尺寸和质量受控的 JPEG。
3. 图片转换在后台执行，保留取消、错误提示和 host 端临时文件路径粘贴流程。
4. 增加小图直通、TIFF 规范化、超限压缩和无效图片数据回归测试。

## Stage 4 验收标准

- 小于上限的 PNG/JPEG 不重新编码。
- 常见大 TIFF 和 PNG 自动转换为不超过 `SendDroppedFiles.maxRawBytes` 的图片。
- 无法解码或无法压缩到上限时显示明确错误，不向 Relay 发送超限 frame。
- Finder 文件 drop、iOS、Relay 和 host 端落盘协议不受影响。

## Stage 5：端到端加密帧预算修复

1. 保持 Relay 的 1 MiB WebSocket frame 上限，不用放宽服务端限制掩盖客户端
   消息预算错误。
2. 将单条文件命令的原始数据预算调整为 512 KiB，为命令内 Base64、ChaChaPoly
   开销、外层 Base64 和 JSON 元数据保留余量。
3. Relay 与客户端共享帧上限及文件预算常量，避免两处数字再次漂移。
4. 增加最终加密 JSON 帧大小的回归测试，而不只验证压缩后图片原始大小。

## Stage 5 验收标准

- 最大允许文件数据形成的最终加密 WebSocket 消息小于 1 MiB。
- 超过新预算的图片继续由现有转换器压缩，不直接触发 WebSocket 断线。
- 粘贴当前约 1.18 MiB PNG 时 session 不重载，host 能收到临时文件路径。
- Finder 文件 drop、普通终端输入和 iOS 链路行为不变。

## Stage 6：macOS 终端视觉统一与 Anysphere Dark

1. 新增独立的 `Anysphere Dark` 终端主题，不覆盖或重命名现有四个主题。
2. 为终端前景、背景和 ANSI 16 色建立单一调色板来源，本地终端、远程终端、
   富文本复制和新 tmux shell 的 OSC 10/11 声明共用该来源。
3. `Default Dark` 背景调整为 macOS 深色窗口背景色，缩小终端与外层工作区的
   视觉断层；其他既有主题保持原配色。
4. 单 pane 不绘制焦点框；多 pane 保留原有焦点高亮和 pane 分隔线，实际
   first responder、键盘路由和 tmux pane 聚焦同步保持不变。
5. 终端工作区外壳直接使用当前终端主题的背景色；标签栏、状态栏、侧栏和窗口
   容器不再各自使用系统 `.bar` 材质。选中标签使用主题前景色的低对比度层级，
   多 pane 焦点框仍保留系统强调色。Sessions 窗口的明暗模式跟随终端主题，
   保证 Light/Dark 主题下系统文字和控件均有正确对比度。
6. 侧栏会话选中高亮作为独立持久化选项，默认关闭；本地与远端会话共用该设置，
   不改变实际会话选择、焦点和键盘路由。

## Stage 6 验收标准

- 设置中的 Theme 可选择并持久化 `Anysphere Dark`，旧配置继续正常解码。
- Anysphere Dark 使用 `#141414` 背景、`#F0F0F0` 前景及对应 16 色调色板。
- Default Dark 终端背景与 macOS 深色窗口背景一致。
- 单 pane 无焦点边框，多 pane 仍以原有蓝色焦点框识别当前输入 pane。
- 本地与远程 Mac 终端呈现相同主题，富文本复制颜色与屏幕一致。
- 终端、标签栏、状态栏、侧栏及窗口背景来自同一主题调色板；切换主题时同步更新。
- 设置可启用或关闭本地及远端侧栏的会话选中高亮，关闭时不影响当前会话状态。

## Stage 7：Anysphere Dark 工作区灰阶层次

1. 保持终端画布、ANSI 16 色、富文本复制及 tmux OSC 颜色不变，Anysphere
   Dark 终端背景继续使用 `#141414`。
2. 在同一主题数据源中增加 Coterm 对应的工作区表面色阶：侧栏 `#141414`、
   工作区 `#181818`、chrome `#202020CC`、选中侧栏行 `#1C1C1C`、边框
   `#303030`。
3. 标题栏、标签栏和状态栏使用 chrome 色，选中标签使用工作区色及 1 px 边框；
   本地与远端复用相同映射。
4. 侧栏会话高亮继续由现有设置控制，启用时使用主题匹配的中性选中颜色；pane
   蓝色焦点框、键盘焦点、tmux pane 同步及输入链路保持不变。

## Stage 7 验收标准

- 终端画布仍为 `#141414`，不改变 Anysphere Dark 的终端输出配色。
- 工作区、chrome、选中表面和边框形成 Coterm 同款灰黑层级，不再整窗铺满
  `#141414`。
- 本地与远端终端工作区使用相同色阶，其他主题保持原有视觉行为。
- 单 pane 无焦点框，多 pane 仍保留原有蓝色焦点框和分隔线。
