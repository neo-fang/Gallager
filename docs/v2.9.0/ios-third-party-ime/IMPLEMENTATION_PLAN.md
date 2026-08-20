# iOS 第三方语音输入兼容修复实施计划

## 状态

- **状态**：✅ 已完成
- **分支**：`hotfix/ios-third-party-ime-context`
- **基线**：Gallager `develop/v2.9.0`；SwiftTerm `0a664be`

## 1. 问题与真机证据

豆包输入法在其他原生文本框中可以持续长语音输入，但在 Gallager iOS 终端中，文本变长或
换行后会主动结束麦克风。

第一轮日志发现 SwiftTerm 会把越界位置钳制到文档首尾。修正该行为并返回真实 caret 后，
真机仍可复现：约 27 个 UTF-16 单元触发近 9000 次 `UITextInput` 查询，期间没有 App 崩溃、
Relay 断线、terminal stream 结束或 first-responder 丢失。因此，越界行为不是充分根因，
第一轮补丁不进入 Gallager 最终依赖。

第二轮把 first responder 改成固定一字符的无文档输入代理，真机仍可复现。诊断日志给出了
更直接的证据：豆包逐字调用 `insertText`，没有 marked text；插入约 9 个字后执行 400 多次
上下文查询，期间 first responder 未丢失。代理的文档长度始终为 1，所以豆包永远读不到
自己刚插入的识别结果，并最终结束语音会话。

## 2. 第三轮方案

保留独立 first responder，但改由原生 `UITextView` 管理真实的影子输入行：

1. UIKit 原生维护第三方输入法可读写的完整上下文，不再手写 `UITextInput`。
2. Gallager 比较影子行与已经发送的文本，只向终端发送最小追加/回退增量。
3. marked text 在提交前不发送，避免把拼音或候选内容重复发给 tmux。
4. 行首保留一个零宽锚点，使空输入行上的 Backspace 仍可到达终端。
5. Enter 发送后重置影子行；不识别输入法品牌，也不修改网络协议。

`UITextView` 透明覆盖终端用于正确排版和上下文计算，但不参与 hit testing；触摸、滚动和复制
仍由原终端视图处理。编辑器作为外层 scroll view 的 viewport 覆盖层约束到
`frameLayoutGuide`，长文本只在编辑器内部滚动，键盘不会再把终端 scrollback 拉到顶部。

## 3. 实施范围

### Gallager

- 新增透明原生 `UITextView` 影子输入代理。
- 增加可测试的输入文档增量计算，支持追加、识别结果修订和 Unicode 字符删除。
- `InteractiveTerminalView` 保留渲染、终端编码和发送职责，不再直接成为 first responder。
- 保留 SwiftTerm 原有 accessory/input view，并将其 reload 转发给输入代理。
- 对 first-responder 切换保持幂等。

### SwiftTerm

- Gallager 继续使用基线 revision `0a664be`。
- 第一轮位置/几何实验保留在独立 SwiftTerm hotfix 分支，仅作为诊断记录，不进入本次部署。

## 4. 验收

- Gallager iOS 无签名和真机签名构建通过。
- 普通中文组合输入、删除、回车、粘贴和输入按键栏功能正常。
- 豆包语音输入超过原复现长度及一屏后，麦克风不再自动消失。
- 输入期间没有 App 退回 session 列表或 terminal stream 重连。

## 5. 真机结果

- 豆包长语音持续输入超过原复现长度和一屏，麦克风未再自动消失。
- 豆包对已识别内容的批量修订可同步为终端 Backspace + replacement，不重复提交整行。
- 普通键盘弹出后，终端仍停留在底部输入位置；长语音不会推动终端 scrollback。
- 用户于 2026-08-13 明确验收通过。
