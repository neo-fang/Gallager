# Stage 31 Code Review Report

## Scope

- Viewer Relay WebSocket generation、发送失败失效和 keepalive 判活。
- terminal stream lease 协议、Host ownership 与 Viewer handler 所有权。
- iOS unexpected stream end 的有界恢复和 Host stop reason 诊断。
- 兼容旧 Viewer/Host 的空 Start/Stop command wire format。

## Findings

### Critical / High / Medium

- None.

### Low

- 已修复：初版删除了原有 `public setTerminalStreamHandler`，会造成源代码兼容性破坏。保留
  deprecated 兼容入口，并让兼容注册同样持有 token；旧式 `nil` 注销不能删除新 API 的后继回调。

## Correctness checks

- receive、ping、registration retry、明文和密文 send 均捕获精确 WebSocket 与单调 generation；
  旧 Task 不能清理或写入替换后的 socket，当前 socket send 失败立即进入同一重连路径。
- 没有为加密发送增加全局串行链；避免把 control command 和 terminal input 引入新的队头阻塞。
- Start/Stop 的 optional lease 编码保持旧空对象格式。Host 只接受当前 lease 的 Stop，连接断开
  仍可按 viewer ID 无条件释放其全部 ownership，不影响其他 Viewer。
- handler registration token 与 stream lease 分离：旧 view 既不能注销新回调，也不能停止新订阅。
- 原有 public handler setter 保留为 deprecated 兼容层，不强迫模块外调用方同步迁移。
- 自动恢复预算按 30 秒滚动窗口限制为两次，稳定 streaming 15 秒后复位；不形成紧密无限循环。
- keepalive 需要连续两轮无任何入站帧才断开，并按 pair ID 只抖动首轮探测；测试注入的短间隔
  不加抖动，避免让测试依赖随机时间。
- Host public `stopStreaming` API 保留；内部 stop reason 只让 pane/capture/resync 异常写 warning。
- 审查中将 handler 复制到局部变量后再调用，避免回调重入注册表时延长字典借用。

## Verification

- 新增/修改测试源码 Swift 6 typecheck 通过，核心 generation、lease 与 recovery 断言运行通过。
- iOS Feature、完整 iOS App 和 macOS Server Feature 构建通过。
- `git diff --check` 通过。
- 完整 Swift package 测试受 GitHub 依赖拉取连续 `early EOF` 阻塞，未产生测试结果；这是合入前
  仍需补齐的验证，不视为测试通过。
- macOS Release 与 iOS device App 已签名、覆盖安装并启动；两端 source revision 均为
  `1fdf353f698f`，Mac CLI 与 tmux 零中断检查通过。

## Assessment

Approved. 代码审查未发现可复现缺陷。实现保持单一 WebSocket/E2EE/terminal stream 架构，
没有增加并行重连器、发送优先级系统或配置面。完整 Swift package 测试仍有明确记录的外部依赖
下载缺口；用户在双端更新后要求合入收尾。
