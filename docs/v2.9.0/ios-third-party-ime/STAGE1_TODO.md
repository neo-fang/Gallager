# iOS 第三方语音输入兼容修复 TODO

## Stage Status

- **Status**: ✅ Completed
- **Progress**: 11/11 tasks
- **Dependencies**: Gallager `develop/v2.9.0` ✅；SwiftTerm `0a664be` ✅

## Tasks

- [x] 真机复现并采集 `UITextInput` 回调证据
- [x] 建立隔离 worktree
- [x] 验证并否定“仅修正 SwiftTerm 越界位置即可解决”的假设
- [x] 恢复 Gallager 的 SwiftTerm 基线依赖
- [x] 实现固定上下文的无文档输入代理并通过真机否定该方案
- [x] 通过诊断日志确认豆包依赖插入后的文档回读
- [x] 改用原生 `UITextView` 维护影子输入行并实现终端增量同步
- [x] 接管 first responder，并保留终端编码、focus 与 accessory 行为
- [x] Gallager iOS 无签名构建通过
- [x] Gallager iOS 签名构建并安装真机
- [x] 豆包长语音、普通输入和键盘弹出滚动真机验收

## Blockers

- 无

## 第一轮实验结论

SwiftTerm `4ae4a02` 修正了位置和几何契约，但真机仍可复现，不能把它当作本问题的修复。
最终 Gallager 改动不依赖该 revision。

## 第二轮实验结论

固定一字符文档不会丢焦点，但豆包看不到自己逐字插入的识别结果，约 9 个字后即结束语音。
第三轮改用原生 `UITextView` 提供真实上下文。
