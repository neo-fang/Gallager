# Stage 14 TODO：远程终端输入与回显延迟

## Stage Status

- **Status**: ✅ Completed
- **Progress**: 8/8 tasks
- **Dependencies**: Stage 4 ✅, Stage 7 ✅

## Tasks

- [x] 建立连续输入、连续输出和顺序保持的聚焦回归测试。
- [x] 将 viewer keystroke 批处理改为 10ms 有界窗口。
- [x] 将 host terminal stream 改为 16ms 固定节拍 flush。
- [x] 构建 Release 并在相同高频输出 pane 重新采样。
- [x] 依据 Release 热点最小化 pipe-pane/MainActor 数据路径开销。
- [x] 完成全量测试、Release DMG 和覆盖安装。
- [x] 禁止 iOS 终端文本复制 sheet 弹出或继承键盘，并保持关闭前后的输入状态。
- [x] 由用户在异机 Mac viewer 与 iOS viewer 完成真实远程输入手感验收。

## Decisions

- 不做 terminal 本地回显；host PTY 始终是唯一显示真相。
- 不删除发送串行化；只去掉会被连续事件无限重置的尾随等待。
- 输出 flush 采用固定节拍，不使用自适应算法或新协议字段。
- 并发优化保持 `PipePaneReader` actor 拥有解析状态，MainActor 只接收有序结果。
- Release 测量与 Debug 定位严格分开。

## Baseline Evidence

- Mac 和 iOS viewer 控制同一本机 host 均出现输入回显延迟，问题位于共用链路。
- `KeystrokeDebouncer` 为 30ms 尾随 debounce，`TerminalStreamService` 为 50ms
  尾随 debounce，单字符已有约 80ms 人为等待。
- 当前已安装 Debug App 在 Codex 高频输出时约占用一个 CPU core；采样显示
  `PipePaneReader.processIncomingData` 多轮过滤和 MainActor 数据路径活跃。
- relay/nginx 运行正常且无明显发送队列积压，暂不修改 relay。

## Blockers

- None.

## Verification

- 10ms viewer 有界批处理与 16ms host 固定节拍测试通过；连续事件不会重置首个
  事件的发送截止时间，raw input 与普通按键继续走同一 FIFO 发送队列。
- pipe-pane 聚焦测试覆盖 plain-data 快路径、buffer/live 顺序和跨 chunk OSC 恢复；
  Viewer WebSocket JSON 解码移到独立 actor，消息处理仍由单一 receive loop 保序。
- 聚焦测试：20 项批处理/pipe-pane 测试通过；OSC、pipe-pane 与 Viewer 活性相关
  52 项测试通过。
- 完整 Swift package：1,585 tests / 219 suites 通过。
- macOS `Release` 与 iOS Simulator `Debug` 均编译通过；macOS Release 深度签名校验通过。
- 同一 211 × 59、50Hz 单行刷新 pane 的 Release CPU：基线 19 个有效样本平均
  66.0%、峰值 101.6%；候选两轮平均分别为 38.1% 和 46.1%，合并平均约 42.1%，
  相对基线下降约 36%。残余热点主要仍是 SwiftTerm/SwiftUI 绘制，不继续扩大改动范围。
- `Gallager-2.7-zengjice.dmg` 已实际挂载验证：版本 2.7 (40)，无 Debug/Preview
  dylib，内置 CLI 可启动；SHA-256 为
  `72e7f2da191fa71ba149352a75371ed4c6142942e192867c0306cf0720d5297c`。
- 已从该 DMG 覆盖安装 `/Applications/Gallager.app`；`wait-ready` 与 `ping` 分别返回
  `ready`、`pong`。
- iOS 真机包已使用本地 provisioning profile 分层签名并覆盖安装到 `ZengJice iPhone`；
  Bundle ID 为 `com.zengjice.gallager.local`，版本 2.7 (40)，安装后已成功启动。
- 复制 sheet 焦点策略的 3 项测试通过；iOS 真机重新编译、签名、覆盖安装并启动成功。
  sheet 存续期间 toolbar 与多 pane 两种模式都强制关闭底层 terminal input。
- 用户已完成异机 Mac/iOS 真机远程输入与复制 sheet 验收，结果通过。
