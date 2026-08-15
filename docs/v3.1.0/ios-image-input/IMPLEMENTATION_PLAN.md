# CtrlX 3.1.0：iOS 图片输入

## 问题

iOS Viewer 目前只能向终端发送键盘和文本输入。用户无法从照片中选择图片并交给远端
Agent；macOS Viewer 已有图片粘贴能力，但这条能力尚未暴露给 iOS。

## 设计

1. iOS 当前 window 的工具栏提供系统照片选择入口，一次选择一张图片。
2. 图片读取完成后在后台规范化为 PNG/JPEG；超过 Relay 预算时逐级缩放、压缩。
3. 继续使用现有 `SendDroppedFiles` E2EE 命令。Host 将图片保存到 `$TMPDIR/ctrlx-drop-*`
   并把 shell-safe 路径 bracketed-paste 到当前 pane。
4. 选择图片时固定目标 pane；异步处理期间切换 pane 不允许把图片误投到新 pane。
5. 上传期间禁用重复选择并显示进度；读取、压缩、断线和 Host 拒绝均显示明确错误。
6. 不自动发送 Enter。Agent 或 shell 只收到一个本机可读图片路径，由用户决定何时提交。

## 实施范围

- 将 macOS 已验证的图片规范化器下沉到 `ClaudeSpyCommon`，供 macOS/iOS 共用。
- 在 iOS `WindowLayoutView` 接入 `PhotosPicker`、上传生命周期和错误反馈。
- 增加图片格式识别、透传、压缩和无效输入的纯逻辑测试。
- 验证共享 Swift 测试、macOS 构建和无签名 iPhoneOS arm64 构建。

## 非目标

- 不增加多图、文件选择、相机拍摄或图片编辑。
- 不读取整个照片库，也不新增照片库权限声明。
- 不修改 Relay 协议、部署配置、Host 落盘规则或 tmux 输入语义。
- 不把图片直接编码为终端 escape sequence，也不自动执行 Agent prompt。

## 验收标准

- iOS 可从照片中选择一张 PNG、JPEG 或 HEIC 图片并投递到当前 pane。
- Host 收到的路径指向可读取的 PNG/JPEG 文件，路径以一次 bracketed paste 写入且不带 Enter。
- 大图自动压缩到 `SendDroppedFiles.maxRawBytes`；无法压缩时不发送并提示原因。
- 读取或上传期间不能重复发送；页面退出会取消未完成任务。
- 多 pane 场景固定发送到选择图片时的 active pane，不随之后的焦点切换漂移。
- 断线、pane 不存在和网络失败均结束 loading，不留下不可恢复状态。
- 现有 macOS 图片粘贴行为与传输协议保持兼容。

