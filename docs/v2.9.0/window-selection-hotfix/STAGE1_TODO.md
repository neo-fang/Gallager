# Window 点击切换回归修复 TODO

## Stage Status

- **Status**：✅ Completed
- **Progress**：5/5 tasks
- **Dependencies**：Window 拖拽排序修复 ✅

## Tasks

- [x] 定位本地与远程点击回归根因
- [x] 修正稳定 ID 调用契约
- [x] 增加本地与远程真实 tmux active Window 断言
- [x] 运行相关测试与 macOS 构建
- [x] 完成本地与双 Mac 远程真实点击验收

## Blockers

- 当前无阻塞。

## Verification

- 14 项聚焦测试通过：Terminal Window 导航、稳定 Window 身份及真实隔离 tmux 重排。
- macOS Debug App 构建与深度签名校验通过。
- 本地真实 App：点击 `winC` 后 tmux active Window 变为 `winC`，前后快捷键切换通过。
- 双 Mac Host/Viewer：Viewer 点击 `winC` 后 Host 的 tmux active Window 变为 `winC`，快捷键切换通过。
- 两套旧 Tab Reorder 场景均继续通过拖拽与 active Window 阶段，之后被既有 `terminal 1` 精确 UI
  文本断言阻塞；真实 tmux 顺序断言已经通过，该失败与本 hotfix 无关。
