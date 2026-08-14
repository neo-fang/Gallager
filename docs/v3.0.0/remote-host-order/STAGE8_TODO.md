# Viewer 远程 Host 顺序 TODO

## Stage Status

- **Status**: ✅ Completed
- **Progress**: 6/6 tasks
- **Dependencies**: Stage 7 远程 session 顺序 ✅

## Tasks

- [x] 明确 Host 顺序的数据归属、拖放语义和协议边界
- [x] 实现共享 Host 移动算法及边界测试
- [x] 实现 macOS 持久化与 sidebar Host 拖拽
- [x] 实现 iOS 持久化与 Edit 模式 Host 拖拽
- [x] 验证配对更新、删除、重启和双端构建
- [x] 完成 Stage 8 code review 与验收记录

## Blockers

- 当前无阻塞项。

## Verification

- `RemoteHostOrder` 与设置持久化测试：4/4 通过。
- macOS `ClaudeSpyServer` App target 构建通过。
- iOS `ClaudeSpy` generic-device App target 构建通过。
- `git diff --check` 通过。
- Stage 8 review 未发现 P1/P2/P3 问题。
- 真实 macOS/iOS 拖拽交互保留为安装后的人工验收项，不阻塞源码集成。
