# Gallager 2.8.0 实施计划

## 状态

- **状态**：✅ 已完成
- **进度**：13/13 stages

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

## Stage 12：Agent 工作进度兜底

### 目标

让不发送 `OSC 9;4` 的 Codex 等 agent 在工作时也显示蓝色 indeterminate
进度条，同时保留终端程序报告的真实百分比、警告和错误进度的最高优先级。

### 实施范围

1. 在共享 `PaneState` collection 上派生 session 有效进度：先选择第一个真实
   `progress`，仅在所有 pane 都没有真实进度时才以 working agent 兜底为
   `.indeterminate`。
2. macOS 本地 sidebar、Mac viewer、iOS session list 及对应 accessibility proxy
   全部读取同一派生规则。
3. 不解析 Codex terminal title spinner，不新增 relay 字段或可变 UI 状态。
4. 增加无进度、working fallback、非 working 隐藏、多 pane 真实进度优先测试。

### 运行时前置条件

Codex 必须先安装 Gallager agent 插件，并在安装后新建会话，才能通过 hook
产生 `.working` 状态。仅靠进程检测发现的 Codex 会话保持 `.idle`，不会触发
fallback；Claude Code 原生 `OSC 9;4` 进度不受此条件影响。

### 验收标准

- 已安装 Gallager agent 插件的 Codex 进入 `.working` 且没有 `OSC 9;4` 时显示
  蓝色滚动条，完成或等待输入后隐藏。
- 任意 pane 存在真实 terminal progress 时，真实值优先于其他 pane 的 agent fallback。
- Claude Code 的 `OSC 9;4`、`gallager set-progress`、Mac viewer、iOS 和无障碍值不回归。
- 共享聚焦测试以及 macOS/iOS 构建通过。

## Stage 13：macOS Window 标签双击重命名

### 目标

让 macOS 本地 host 与 Mac viewer 的 terminal window 标签支持双击打开现有
`Rename Window` 输入框，减少必须打开右键菜单的操作成本。

### 实施范围

1. 复用 `WindowRenamingModifier` 的同一份输入状态和保存逻辑，不创建第二套重命名 UI。
2. 本地与远程 window 标签主按钮检测 macOS 双击；单击继续选择 window，双击打开
   预填当前名称的输入框。
3. 远程 host 断开时保持重命名禁用；本地及已连接远程会话继续调用现有 rename 闭包。
4. 右键 `Rename Window`、关闭按钮、split 按钮、拖拽排序以及 file/browser/Git 标签行为不变。
5. 更新 Window Rename E2E 场景，使 host 与 Mac viewer 都覆盖真实双击路径。
6. 不修改 tmux、relay、共享命令协议或 iOS 行为。

### 验收标准

- 双击本地 terminal window 标签后出现预填当前名称的 `Rename Window` 输入框。
- 双击 Mac viewer 的远程 terminal window 标签后出现相同输入框，保存后 host 与 viewer 同步。
- 单击仍只选择 window；右键菜单仍可重命名。
- 断开的远程 host 不允许发起重命名。
- close/split/drag 及非 terminal 标签不误触发重命名。
- Window Rename E2E 编译、受影响 Swift tests 与 macOS 构建通过。

## Stage 14：远程终端输入与回显延迟

### 目标

降低 Mac/iOS viewer 控制 Mac host 时的按键到回显延迟，并保证 Codex 等 TUI
持续高频输出时输入仍能及时显示。优化必须保持按键、raw input 和 terminal stream
顺序，不使用会与真实 PTY 状态分叉的本地字符预回显。

### 实施范围

1. 远程按键批处理从 30ms 尾随 debounce 改为首个按键启动、后续按键不重置的
   10ms 有界窗口；现有单消费者发送队列继续保证 keystroke/raw input 顺序。
2. host terminal stream 从 50ms 尾随 debounce 改为首个数据块启动、后续数据不
   重置的 16ms 固定节拍；达到 8KiB 时立即 flush，并取消已经失效的定时任务。
3. 增加连续输入和连续输出测试，证明数据流持续到来时仍会在窗口上界内发送，
   同时覆盖取消、立即 flush 和操作顺序。
4. 所有性能测量和可分发 DMG 使用 macOS Release 构建；Debug 仅用于定位，不作为
   验收结果。记录同一 211 × 59 Codex 高频刷新 pane 的 CPU 和采样热点。
5. Release 采样若仍显示 pipe-pane/MainActor 数据投递为热点，则只合并连续数据
   delegate 投递，并减少确定没有相关转义序列时的多轮全量扫描；不得改变 terminal
   字节顺序、OSC 事件语义或 relay 协议。
6. viewer 的 UI 状态和 SwiftTerm feed 仍在 MainActor；只允许不可变 `Data` 在 actor
   边界传递，不使用 `Task.detached`、`nonisolated(unsafe)` 或伪造 `Sendable`。
7. iOS 复制终端文本 sheet 弹出前同步释放终端 first responder，并在 sheet 存续期间
   禁止底层 terminal 因视图更新重新激活；关闭时只恢复弹出前已开启的输入状态。

### 验收标准

- 单个远程按键的人为等待上界从约 30ms 降至 10ms；连续输入不会等到停止打字才发送。
- terminal stream 人为等待上界从约 50ms 降至 16ms；持续输出仍按固定节拍发送。
- keystroke、raw input、initial state、增量数据及 stream end 的顺序保持不变。
- Codex 高频输出时 Mac/iOS viewer 可持续输入，不出现成批延迟回显或主线程长时间阻塞。
- 聚焦测试、完整 Swift package 测试、macOS/iOS 构建和 macOS Release 签名校验通过。
- 生成的 `Gallager-2.7-zengjice.dmg` 来自 Release 产物，覆盖安装后可正常连接和输入。
- iOS 复制终端文本 sheet 内可滚动、选中和复制，但不会显示系统键盘；关闭后终端输入
  状态与弹出前一致。

## Stage 15：iOS 终端输入控件位置

### 目标

让 iOS 终端的键盘显示/隐藏入口可在原有右上角与底部安全区之间切换，
既保持升级用户的原有布局，也提供更易点击且不与终端手势冲突的底部选项。

### 实施范围

1. 新增持久化的 `Keyboard Control Position` 枚举设置，只允许 `Top Right` 与
   `Bottom Bar` 两个互斥值；默认 `Top Right`，非法旧值也回退到该值。
2. 在 iOS `Settings > Terminal` 使用原生两项 Picker 绑定该设置，切换后当前终端
   页即时更新，不重连 host 或 relay。
3. 保留轻量的共享底部键盘控件：键盘隐藏时显示 `Input`，键盘显示时
   显示 `Hide Keyboard`，使用系统按钮样式和不小于 44pt 的点击区域。
4. 单终端页在 `Top Right` 时使用原导航栏按钮，隐藏导航栏时使用原右上角
   悬浮按钮；`Bottom Bar` 时只通过 `safeAreaInset(edge: .bottom)` 展示底部控件。
   复制按钮保持原有位置。
5. 多 pane 页由父视图根据同一设置只展示一个右上角或底部控件，键盘输入
   继续只发送到当前选中 pane；无选中 pane 或 host 断开时禁用。
6. 保留现有终端滚动、双击、长按、粘贴/选择及 tmux mouse mode 手势，不给终端
   内容区增加键盘触发事件。
7. 保留 Stage 14 的复制 sheet 焦点隔离；sheet 打开时不得弹出键盘，关闭后按原有
   规则恢复输入状态。
8. 不修改 relay、host、tmux 或 agent 协议，不引入自动弹出键盘或新手势。

### 验收标准

- 未保存设置或保存值无效时默认 `Top Right`，显示升级前的右上角按钮。
- 切换到 `Bottom Bar` 后，键盘隐藏时底部安全区显示 `Input` 控件；点击后键盘
  弹出，控件随键盘上移并
  变为 `Hide Keyboard`。
- 两种位置始终互斥；设置切换即时生效，不断开当前 host 连接。
- 单终端、隐藏导航栏和多 pane 页均遵循同一位置设置。
- 多 pane 切换后，只有选中 pane 接收输入；host 断开时不能启用键盘。
- 终端滚动、双击、长按、粘贴/复制及 tmux mouse mode 行为不回归。
- 复制 sheet 依旧不弹出键盘，关闭后输入状态恢复正确。
- 聚焦测试、Swift package 测试、iOS device 构建和 iPhone 真机验证通过。

## Stage 16：远程终端首屏原子揭示

### 目标

修复 Mac viewer 打开或重连远程 pane 时可见的历史内容回刷，并减少多 Viewer
场景下发送给未订阅设备的终端流量。继续只维护正在查看的 pane，不让后台 window
常驻 SwiftTerm 或实时流。

### 实施范围

1. `PipePaneReader` 的 buffer flush 增加真实完成屏障：返回时此前缓存的 delegate
   事件必须已经按顺序投递，不再把 fire-and-forget 当作完成。
2. `TerminalStreamService` 在 initial state 之后排空 bootstrap 数据并立即发送未满的
   16ms batch；只有该内部屏障通过后，`StartTerminalStream` 才返回成功。
3. initial state、title 和增量 terminal 消息只发送给实际订阅该 pane 的 Viewer；
   push notification 和 session state 的广播语义不变。
4. Mac viewer 在连接阶段缓存 initial state 与随后到达的 bootstrap 原始字节，不逐块
   feed SwiftTerm；同时收到有效 initial state 和 start command success 后，以一次
   feed 建立最终首屏，再揭示 terminal、恢复滚动与焦点。
5. Mac viewer 始终将 SwiftTerm 行列锁定为 Host 报告的 pane 尺寸，布局变化不得按
   Viewer 窗口高度重算 terminal rows；尺寸变化只接受 Host 的 dimension change。
6. 保留现有约三屏 scrollback、8KiB/16ms 实时批处理和当前 pane 按需订阅；不使用
   固定等待、ANSI 启发式去重、后台 window 常驻或本地伪回显。
7. 保持现有网络消息格式，复用 `StartTerminalStream` command response 作为 ready
   信号；旧 Host 仍可连接，新 Host 提供更严格的 bootstrap 完成语义。
8. 增加 flush 屏障、initial/data/ready 顺序、多 Viewer 路由、重试及持续输出期间
   首屏揭示的聚焦测试，并以 Release-like 构建验证切换和高频输出行为。

### 验收标准

- Mac viewer 打开、切换或重连远程 pane 时不显示历史内容逐段回刷；首屏准备完成后
  只执行一次 bootstrap feed 并一次出现，且当前终端内容正确。
- Viewer 布局与窗口尺寸变化不会改写 Host pane 的 terminal rows，也不会触发额外的
  SwiftTerm resize/layout 反馈。
- `flushBuffer()` 完成前的所有数据先于 bootstrap ready；未满 8KiB 的末尾 batch
  不等待常规 16ms 定时器。
- 新 Viewer 的 initial state 不再刷新无关 Viewer；增量数据不发送给未订阅该 pane
  的已连接设备。
- 持续输出期间 `StartTerminalStream` 不饥饿、不依赖输出静默，也不因固定延时增加
  首屏等待。
- 切换 window 后只有可见 pane 保持 Viewer 端渲染；CPU、内存和网络不会随后台
  window 数量线性增长。
- 聚焦测试、完整 Swift package 测试和 macOS Release 构建通过；相同远程切换场景
  完成真机/双 Mac 验收。

## Stage 17：Mac Viewer 交互延迟与主线程公平性

### 目标

修复相同网络、Host 和远程 pane 下，Mac Viewer 输入回显明显慢于 iOS Viewer 的问题。
Mac 同时运行本地 sessions 时仍须优先保证交互响应；允许为此使用更多但有界的计算资源，
不得使用会与真实 PTY 状态分叉的本地预回显。

### 实施范围

1. 固定同一远程 pane 的逐字输入场景，分别记录 Viewer 按键回调、按键批次进入发送、
   Host 命令执行、回显 stream 到达和 SwiftTerm feed 边界；区分网络 RTT、Host 执行和
   Viewer 主线程等待，不以主观感受替代定位证据。
2. 对比 macOS 与 iOS 的共享 `KeystrokeDebouncer`、`ViewerRelayClient` 和 stream 路径，
   只修改能够解释平台差异的部分；不增加 relay 消息、不绕过 E2EE。
3. 审计 Mac 本地 pane 的 pipe reader、session 刷新及 SwiftTerm 渲染是否占用 MainActor，
   对比本地 sessions 空闲、持续输出和完全关闭三种状态，确认输入发送或回显绘制是否
   被无界工作饥饿。
4. 若按键发送被 MainActor 延迟，以一个有序、可取消且有界的交互发送路径替代定时器
   竞争；必须保持文本、Meta/Option、raw mouse input 的 FIFO 语义。
5. 若回显渲染被输出工作延迟，在不丢 terminal 字节的前提下合并每个显示周期的 feed，
   并让每批处理后归还执行权；不得让后台 local window 数量线性增加活跃 View 或任务。
6. 增加确定性测试覆盖单键延迟上界、连续输入、公平调度、取消/重连及字节顺序；性能
   结论使用 Release-like 构建和实际采样，不以 Debug 构建耗时作为验收依据。
7. 生成新的 macOS Release 与固定名称 DMG，在同一远程 pane 上完成 Mac/iOS 对照和
   本地 sessions 负载下的双 Mac 真机验收。

### 验收标准

- 同一网络与远程 pane 下，Mac Viewer 不再出现平台自身造成的可感知按键停顿；网络和
  Host 时间相同时，按键发送与回显 feed 不被本地 session 工作长期阻塞。
- 持续输入不会等到停止输入才发送；持续输出期间交互工作不会被输出队列饥饿。
- 文本、Meta/Option、控制键、raw mouse input 和 terminal 字节顺序保持不变。
- 不使用本地预回显、固定静默等待、无界 Task 或为每个字符长期创建独立后台任务。
- 聚焦测试、完整 Swift package 测试、macOS Release 构建、签名与 DMG 校验通过。
- 相同远程场景完成 Mac/iOS Viewer 对照，且 Mac 同时存在本地 sessions 时体验符合预期。

## Stage 18：macOS 本地终端输入延迟

### 目标

缩小 Gallager 本地 terminal 与直接 `tmux attach` 的输入响应差距。复用本地 pane
已经建立的 tmux control-mode 持久连接发送交互按键，避免为每个按键批次启动新的
`tmux send-keys` 进程，同时保持按键顺序、Meta/Option 组合和失败回退语义。

### 实施范围

1. 以本地 terminal 逐字输入为固定场景，记录现有进程式 tmux 命令开销；当前基线为
   200 次 tmux 客户端调用约 0.79 秒，平均约 3.95ms/次，不把该数据误当作完整回显延迟。
2. 在现有 `TmuxControlClientManager` 上增加窄接口，仅为已经订阅且持有 control-mode
   连接的本地 pane 发送交互按键；不得把任意未转义文本拼接为 tmux 命令。
3. 本地 `InteractiveTerminalView` 输入优先走持久连接；连接不存在、已失效或命令失败时，
   回退到现有 `TmuxService.sendKeystrokes` 进程路径，避免输入静默丢失。
4. 持久路径和回退路径共用一个有序发送边界，保证普通文本、命名键、Meta/Option 组合
   以及连续输入的 FIFO 顺序；不添加本地预回显。
5. 增加命令编码、持久连接命中、故障回退和输入顺序测试；先验证第一阶段收益，只有仍有
   可测量延迟时才考虑让普通按键绕过 `KeystrokeCoalescer` 的 runloop 合批。
6. 不修改 remote viewer、relay、host 命令协议、pipe-pane 输出、SwiftTerm 渲染或插件
   使用的通用 `TmuxService` 命令路径。

### 验收标准

- 已建立本地 pane stream 时，连续交互输入不再为每个批次启动新的 tmux 客户端进程。
- control-mode 尚未就绪或执行失败时，输入按原顺序通过进程路径重试一次，不静默丢失，
  不因重试造成正常路径重复输入。
- literal 文本、空格、Enter、方向键、Escape、Unicode 和 Meta/Option 组合行为不回归。
- 不使用本地预回显、固定等待、无界 Task 或新的长期后台轮询。
- 聚焦测试、完整 Swift package 测试和 macOS Release 构建通过。
- 使用 Release 产物在同一 local session 对比直接 `tmux attach`，确认主观输入停顿改善；
  若仍存在差距，再以测量证据决定是否进入第二阶段按键合并优化。
