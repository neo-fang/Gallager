# Stage 16 TODO：远程终端首屏原子揭示

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 6/7 tasks
- **Dependencies**: Stage 14 ✅

## Tasks

- [x] 为 `PipePaneReader` 增加有序 flush 完成屏障。
- [x] 在 `TerminalStreamService` 排空 bootstrap batch 后再完成 start command。
- [x] 将 terminal stream 路由限制到实际 pane 订阅者。
- [x] 让 Mac viewer 后台装载首屏并在 ready 后一次揭示。
- [x] 增加顺序、多 Viewer、重连和持续输出聚焦测试。
- [x] 完成完整测试和 macOS Release 构建。
- [ ] 完成相同远程场景的双 Mac 真机验收。

## Decisions

- 保持只有当前可见 pane 维持 Viewer 端实时流，不缓存所有 window 的终端 View。
- 保留约三屏 scrollback，不以牺牲复制和历史浏览换取首屏速度。
- 不增加网络协议消息，使用现有 start command success 作为 bootstrap ready。
- ready 由 FIFO 屏障产生，不使用静默检测或任意毫秒等待。
- 只隐藏 bootstrap 的中间渲染，不丢弃捕获期间的数据。

## Blockers

- None.

## Verification

- 聚焦测试：29 tests / 7 suites passed。
- 完整 Swift package：1601 tests / 222 suites passed。
- macOS Release：`ClaudeSpyServer` arm64 构建通过。
- 产物：`Gallager.app` 2.7 (40)，主程序与 `GallagerCLI` 均为 arm64。
- 签名：Apple Development，`codesign --verify --deep --strict` 通过。
- 待验收：相同远程 pane 的打开、切换、重连及持续高输出双 Mac 真机测试。
