# iOS 第三方语音输入兼容修复 Review

## Scope

- iOS terminal first responder 与第三方键盘上下文。
- 原生影子输入行、终端增量同步和 IME marked text 边界。
- 键盘弹出时的内外层 terminal scroll view 行为。
- SwiftTerm accessory、terminal focus 和 responder chain 兼容性。

## Findings

### Critical / High / Medium

- None。

### Low

- 已修复：固定一字符的无文档代理无法让豆包回读已插入结果，导致数百次上下文查询后结束语音。
- 已修复：透明 `UITextView` 初版位于 terminal scrollback 内容坐标，键盘会为 caret 自动滚到顶部。
- 已修复：影子编辑器作为 terminal 的兄弟视图后，硬件按键 responder chain 可能绕过 terminal；
  现显式把未处理事件转回 `InteractiveTerminalView`。

## Correctness Checks

- `UITextView` 是唯一 UIKit 文档实现；没有第二套手写 `UITextInput` 位置、范围或几何逻辑。
- marked text 在 commit 前不发送；已提交文本只通过最长公共前缀计算最小后缀改写。
- 删除按 Swift `Character` 计数，复合 emoji 不会被拆成多个终端 Backspace。
- 空行零宽锚点只用于保持 Backspace；不会进入发送给 tmux 的 payload。
- Enter 先发送再重置本地影子行，不清理 tmux session 或 terminal scrollback。
- 影子编辑器固定在外层 `frameLayoutGuide`，自行滚动长文本且不参与触摸命中。
- SwiftTerm revision、Relay wire model、E2EE 和 tmux 数据均未修改。

## Verification

- Gallager iOS generic Debug 构建通过。
- Apple Development 真机签名、codesign 校验、覆盖安装及启动通过。
- 聚焦输入增量测试覆盖追加、语音识别修订、复合 emoji 删除和换行重置。
- `git diff --check` 通过。
- iPhone 真机使用豆包长语音超过一屏通过；键盘弹出后 terminal 输入位置保持可见。

## Residual Risk

- 第三方语音输入可能大幅修订较早的识别结果，终端必须以多个 Backspace 重写对应后缀；这是
  在不改变 tmux 协议的前提下保持远端输入行一致的必要成本。
- 影子行描述当前 terminal 输入段，不解析远端 TUI 的语义；外部程序主动移动光标时，键盘中途
  修改历史识别结果仍以“终端光标位于输入末尾”为前提。

## Assessment

Approved。未发现 P3 及以上遗留问题；实现用 UIKit 原生文档替代手写协议，并保持网络和 tmux
边界不变。用户已完成长语音及键盘滚动真机验收。
