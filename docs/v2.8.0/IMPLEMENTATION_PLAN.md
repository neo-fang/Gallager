# Gallager 2.8.0 实施计划

## 状态

- **状态**：🟡 进行中
- **进度**：10/11 stages

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

## Stage 3：Terminal Payload 缓存性能

### 目标

降低 Codex 等高频刷新 TUI 在 Mac viewer 中的持续 CPU 占用，同时保持 OSC 8
超链接识别、普通 URL 检测和终端渲染行为不变。

### 实施范围

1. 让共享 payload 缓存识别输入数据中真正的 OSC 8 起始序列，并正确处理序列跨
   数据块拆分的情况。
2. 只有出现新 OSC 8 序列时才扫描终端缓冲区并提取 payload；普通终端输出仅校验
   已缓存的链接单元格，不再遍历所有行列。
3. macOS 与 iOS 共用同一优化路径，不增加平台分支或 relay 协议字段。
4. 增加序列检测聚焦测试，并用持续刷新的真实 Codex pane 对比修复前后 CPU。

### 验收标准

- 完整及跨数据块拆分的 `ESC ] 8 ;`、C1 OSC 8 序列都能触发 payload 提取。
- 普通文本、CSI 动画和其他 OSC 序列不触发全缓冲区扫描。
- 已缓存链接被普通文本覆盖或被 scrollback 淘汰后仍会失效。
- OSC 8 链接点击、普通 URL 检测和终端显示不回归。
- 聚焦单元测试、macOS 构建通过；同一高频刷新 pane 的空闲观察 CPU 明显下降。

## Stage 4：Host Terminal Stream 生命周期

### 目标

修复 Mac host 在高吞吐输出及 iOS/Mac viewer 重连或重试时出现的 terminal stream
初始化失败和订阅泄漏。同一 viewer 对同一 pane 的重复 Start 必须幂等，首个实时
增量不得早于完整 initial state，失效的 tmux subscription 必须能被清理并重新创建。

### 实施范围

1. 保留命令来源 viewer 的 pairId，并将其传入 host terminal stream 生命周期。
2. 将按次数增减的订阅计数改为按 viewer ID 管理的集合；重复 Start/Stop 不改变
   其他 viewer 的所有权。
3. 复用已有 stream 时若无法读取当前 pane 内容，立即清理失效 subscription，允许
   viewer 的下一次有限重试创建新 stream。
4. 在订阅前建立有序增量缓冲，但只在 initial state 发送完成后启动消费，确保高吞吐
   输出不会抢占初始化消息或拖延 Start command response。
5. 不修改 relay 协议，不增加轮询或无限重试。
6. 增加订阅所有权聚焦测试，并验证 macOS host 构建及 loffice 真机链路。

### 验收标准

- 同一 viewer 连续 Start 同一 pane 只保留一个订阅所有权。
- 一个 viewer Stop 不会终止其他 viewer 正在使用的 stream。
- 最后一个 viewer Stop 后释放 `PaneStreamManager` subscription 并发送 streamEnd。
- 旧 stream 无法捕获 pane 内容时会被清理，后续 Start 可重新订阅。
- pane 在持续高吞吐输出时，iOS 仍先收到 initial state，再按序收到订阅期间缓存的
  增量，Start 命令不因实时输出占满发送链而超时。
- iOS 连接 loffice 后可打开并输入 terminal，高吞吐任务期间不再出现 `Stream Error`。
- 聚焦测试、Swift package 测试与 macOS 构建通过。

## Stage 5：Agent Pane 进程校准

### 目标

修复 tmux pane 的普通终端与 coding agent 图标偶尔分类不准的问题。插件 hook 继续
提供实时、权威的 agent 生命周期；host 每 10 秒扫描一次 pane 进程树，只校准 hook
缺失或进程异常退出造成的遗漏和残留。

### 实施范围

1. 保持现有显示语义：session 行在任意 window/pane 存在 agent 时显示 agent 状态，
   window 标签只反映自身 panes。
2. 在 `MirrorWindowManager` 单独维护进程扫描推断的 pane 所有权，不让扫描结果覆盖
   或删除插件 hook 已确认的会话。
3. 每 10 秒调用现有 `TmuxService.detectAgentPanes`；新增 agent 时补标，已退出的扫描
   推断 agent 及时撤销。
4. 插件报告 session end 后，在旧进程仍处于退出阶段时抑制扫描复活；进程消失后
   自动解除抑制。
5. 仅当分类实际变化时更新防休眠状态并向 viewer 推送 session state。
6. 周期任务随 host 生命周期启动和取消，不改变现有 5 秒 pane/session 校验间隔。

### 验收标准

- 在已有普通 shell pane 中启动 Codex/Claude 后，最迟 10 秒出现正确 agent 图标。
- 没有 hook 的 agent 进程退出后，最迟 10 秒恢复普通终端图标。
- hook 已确认的 agent 不会因某次进程扫描漏报而被清除。
- hook 报告结束后，仍在退出中的旧进程不会把 agent 图标重新标回。
- 同一 session 的左侧状态与每个 window 标签继续使用原有聚合层级。
- 无分类变化时不推送重复 session state；聚焦单元测试与 macOS 构建通过。

## Stage 6：Session 名称展示优先级

### 目标

让 rename session 后的真实 tmux `session_name` 在 Mac 和 iOS 的默认会话界面
立即可见。Description 继续作为独立元数据，但不再遮住 session 身份。

### 实施范围

1. Mac 的 agent 和普通终端默认 Sidebar Fields 均将 `sessionName` 放在首位。
2. 仅对完全等于旧默认数组的已保存配置执行一次性迁移；不改写用户自定义顺序。
3. iOS agent/普通终端行以 `sessionName` 为主标题，Description 和项目/目录信息作为副标题。
4. iOS 会话详情页标题固定使用 `sessionName`，window/terminal 标题继续在标签内展示。
5. Mac 菜单栏的 agent 和 terminal-only 行使用真实 `sessionName`，不再以 Description 取代。
6. 增加默认值、旧配置迁移和共享展示规则的聚焦测试，并验证 macOS/iOS 构建。

### 验收标准

- 存在 Description 时，Mac 和 iOS 默认会话行仍以新 `sessionName` 为主标题。
- Description 仍可编辑、清空并显示为副信息，不与 rename 互相改写。
- 旧默认 Mac 配置自动升级；非默认的自定义 Sidebar Fields 保持原样。
- iOS 会话列表和详情页、Mac 本地/远程侧边栏及菜单栏均能看到改名结果。
- 聚焦单元测试、Swift package 测试与 macOS/iOS 构建通过。

## Stage 7：macOS Terminal 行级渲染缓存

### 目标

降低本地和远程 Mac terminal 在 Codex spinner、进度条等小范围高频刷新时的主线程
占用。终端内容更新不再误触发 AppKit/SwiftUI 布局，只重建发生内容或视觉状态变化
的可见行，并避免对未变化行重复执行 URL 检测和 CoreText 排版。

### 实施范围

1. 以同一 211 × 59 Codex spinner pane 的 Release CPU、绘制行数和主线程采样作为
   固定基线；临时测量代码不得进入发布路径。
2. 在 SwiftTerm CoreGraphics renderer 中缓存每行的 `ViewLineInfo`、`CTLine` 和
   glyph runs；缓存键使用 `BufferLine` identity、generation、绝对 row 和列数。
3. 将 terminal feed、滚屏和 range change 产生的 URL 装饰更新从 `needsLayout` 中
   拆出，在主线程下一轮合并执行；真实尺寸变化仍走原有 layout 路径。
4. 对可见行的 URL 检测结果增加同样由 line identity、generation、绝对 row 和列数
   约束的缓存；只保留当前 viewport，避免每个输出块扫描整屏。
5. selection、hover link、Command-link highlighting 等不由 `BufferLine.generation`
   表达的交互状态直接绕过缓存；字体、主题、ANSI palette 和渲染选项变化时清空缓存。
6. 缓存只保留可见行，避免 scrollback 增长导致内存无界增长。
7. 不修改 terminal feed 数据、relay、tmux stream 和 iOS 渲染逻辑；不重新引入 Stage 3
   已验证无稳定收益的降帧、数据合并或默认 Metal renderer。
8. 增加缓存命中、内容变更、行替换、交互状态绕过和全局失效的聚焦测试，并以相同
   Release 场景比较 CPU、主线程热点和输入响应。

### 验收标准

- 只有 Codex spinner 一行变化时，未变化可见行不再调用 `buildAttributedString` 或
  `CTLineCreateWithAttributedString`。
- 普通输出、滚屏、alternate buffer、窗口 resize、字体/主题切换、链接和文本选择
  显示正确，无陈旧行或残影。
- 缓存容量与可见行数绑定，scrollback 持续增长时缓存不无限增长。
- terminal feed、滚屏和 range change 不再请求完整布局；URL 下划线仍在下一轮主线程
  更新，窗口尺寸变化仍触发原有布局和 resize。
- 同一 211 × 59 Release 场景 CPU 相比约 100% 基线明显下降，且主线程绘制样本显著减少。
- SwiftTerm 聚焦测试、Gallager Swift package 测试与 macOS Release 构建通过。

## Stage 8：新建 Window 的 Terminal Stream 竞态修复

### 目标

修复 macOS 本地会话新建 window 后偶发无法及时选中，或键盘输入已经送入 tmux、
Gallager 终端却不再回显的问题。window 数量和 agent 进程校准只能影响复现概率，
不得作为容量限制或通过降低轮询频率掩盖竞态。

### 实施范围

1. `PaneStreamManager` 对每个 pane 的 reader 创建实行 single-flight；按需订阅、
   control-client pane 发现和周期刷新必须等待同一创建任务，不能重复覆盖 FIFO 或
   `tmux pipe-pane`。
2. reader 创建失败后清除 in-flight 状态，允许下一次发现或订阅重试；shutdown 必须
   取消并等待未完成创建，不遗留 FIFO 或 reader。
3. macOS 本地 `New Window` 复用现有 `PaneSurfaceRetry`，等待 `refreshPanes()` 的缓存
   真正包含新 pane 后再选择 window；超出重试预算时显示明确错误。
4. 不修改 tmux 命令协议、relay、iOS terminal stream 和 10 秒 agent 进程校准逻辑。
5. 增加 per-pane reader 并发创建、失败后重试和 stale refresh 后 window surface 的
   聚焦回归测试。

### 验收标准

- 同一 pane 的多个并发 reader 请求只执行一次底层启动，并复用同一 reader。
- reader 首次启动失败后，后续请求可以重新启动，不永久卡在 in-flight 状态。
- 新建 window 与 5 秒 pane refresh、10 秒 pane discovery 同时发生时，最终仍选中新
  window，并建立可持续接收输出的 pipe reader。
- 输入字符在 tmux pane 和 Gallager 镜像中同步出现，不需要切换 tab 或等待下一轮刷新。
- agent 类型校准频率和分类语义不变。
- 聚焦测试、完整 Swift package 测试与 macOS Release 构建通过。

## Stage 9：iOS Terminal 本地文本复制

### 目标

在不改变 host tmux window 尺寸的前提下，让 iOS 能稳定选择并复制当前屏幕之外的
terminal 内容。复制数据直接来自已经渲染的本地 SwiftTerm buffer，不增加 relay
请求，不依赖 host 重新捕获画面。

### 实施范围

1. 保持现有“host 尺寸 terminal + 外层滚动视口”架构，不发送
   `ResizeTmuxPane`，不在 iOS 单方面重排 terminal buffer。
2. 从 `InteractiveTerminalView` 生成静态纯文本快照，覆盖当前可用 scrollback 与
   viewport，并按 terminal 行去掉右侧填充空格。
3. 快照只在用户打开复制界面时生成，不跟随每个 terminal 数据块重建。
4. 增加只读原生文本选择界面，支持系统跨屏选区、复制和全选；关闭后回到实时
   terminal，既有短文本原位选择保持不变。
5. 快照为空时给出明确反馈，不展示无法操作的空白复制界面。
6. 不修改 relay、host、tmux 命令协议和 macOS terminal 行为。

### 验收标准

- iOS 可从 terminal 工具栏打开复制界面，文本来自当前本地 terminal buffer。
- 长内容能在复制界面跨越手机可见区域选择并复制。
- 打开复制界面后 terminal 继续输出也不会移动当前选区；重新打开才生成新快照。
- 普通 buffer、alternate buffer、中文、宽字符和空白行生成可读文本。
- 无可复制文本时显示明确提示。
- iOS 聚焦测试与 Simulator 构建通过；真机复制行为验证通过。

## Stage 10：Agent 快捷回复可靠提交

### 目标

修复 iOS 在 agent 停止后的顶部回复框点击 Send 时，偶发只把正文写入 TUI、却没有
触发 Enter 提交的问题；同时移除未确认实际发送结果就显示并持久化
`Prompt submitted` 的虚假成功状态。

### 实施范围

1. 顶部 reply-after-stop composer 通过已有 command/response 通道直接发送
   `[.text(...), .delay(200), .enter]`，等待 host 完成 tmux 操作，而不是只确认
   WebSocket 写入。
2. Claude Code 与 Codex 的 prompt/reply-after-stop 翻译采用同样的 TUI settle 边界，
   避免正文与 Enter 在同一个输入 burst 中被 TUI 错误消费。
3. reply composer 不再设置或恢复 `promptSubmitted`。host/tmux 对完整输入序列返回
   command success 后清空草稿；连接失败、超时或 tmux 失败时保留。真实 agent state
   进入 `working` 时仍收起 composer 并清理草稿，作为状态通道可用时的兜底。
4. reply 草稿归属于当前 `ResponseState`：`doneWorking → idle` 的 handled 翻转继续
   保留未发送内容，真正进入 `working` 后销毁旧状态；下一轮 composer 使用新的
   lifecycle identity，禁止 SwiftUI/UITextField 复用上一轮的编辑缓存。
5. 不重发 Enter，不修改 blocking form、terminal 键盘或 sidecar 插件协议。
6. 增加 built-in plugin 翻译、延迟边界与 reply composer 状态聚焦测试。

### 验收标准

- iOS 顶部回复框点击 Send 后，host 先写入 literal 正文，等待 200ms，再发送命名 Enter。
- UI 不再显示 `Prompt submitted`；command success 后输入框清空但 composer 保持可用，
  command failure 时保留原文以便重试。
- agent 进入 working 后旧草稿清空；下一轮结束重新出现 composer 时不得恢复上次内容。
- 导航离开再进入 idle/doneWorking session 时，顶部 composer 仍然出现。
- 空 reply-after-stop 仍只发送 Escape；空 prompt 仍不发送任何内容。
- blocking form 与交互式菜单按键时序保持不变。
- Claude Code、Codex、TmuxService 聚焦测试与 macOS/iOS 构建通过。

## Stage 11：iOS Agent 输入模式

### 目标

让顶部 Agent 快捷输入成为可选体验。默认关闭时，进入 Agent pane 只显示 terminal，
既不弹出键盘，也不显示普通 prompt/reply composer；键盘显示/隐藏按钮始终作为
导航栏直接操作，不再收进 Agent 命令菜单，由用户明确决定何时输入。

### 实施范围

1. 在 iOS 设置中增加持久化的 `Agent Quick Input` 开关，默认关闭；升级用户也使用
   相同默认值，不做隐式迁移。
2. 将普通 prompt/reply-after-stop 与 permission/question/plan 等阻塞表单分开处理：
   开关只控制前者，阻塞表单始终可见。
3. 首次进入或切换到 Agent pane 时不自动启用 terminal 键盘。开关关闭时保持只读
   terminal；开启时展示顶部快捷输入，terminal 键盘仍只由导航栏按钮控制。
4. 阻塞表单到达时退出 terminal 键盘输入模式，避免审批 UI 与 terminal first
   responder 争抢焦点；表单处理后不擅自重新弹出键盘。
5. 将 Agent pane 的键盘按钮从 Commands 菜单移到导航栏；Yolo Mode 与 Session Info
   继续留在 Commands 菜单，普通 terminal pane 的按钮行为不变。
6. 不修改 relay、host、tmux 命令协议及 Stage 10 的可靠提交路径。

### 验收标准

- 全新安装或未保存该设置时，`Agent Quick Input` 默认为关闭。
- 默认设置下进入 Agent pane 不自动显示 terminal 键盘，也不显示顶部快捷输入框。
- 开启设置后，进入 Agent pane 默认显示现有顶部快捷输入框；Send 的可靠提交和清空
  行为不回归。
- permission、question、plan approval 等阻塞表单不受开关影响；到达时键盘收起且表单
  可操作。
- Agent 与普通 terminal pane 的 Show/Hide Keyboard 图标都直接显示在导航栏。
- terminal 键盘只响应用户直接操作；切换 pane 或 agent 状态刷新不会自动弹出。
- 展示策略聚焦测试、Swift package 测试、iOS device 构建及 iPhone 真机验证通过。
