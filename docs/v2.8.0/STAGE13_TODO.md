# Stage 13 TODO：macOS Window 标签双击重命名

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 1/5 tasks
- **Dependencies**: Stage 1 ✅

## Tasks

- [x] 确认本地与远程 window 标签共用的现有重命名路径。
- [ ] 让重命名输入框可由标签双击和右键菜单共同触发。
- [ ] 保持单击、断线禁用、close/split/drag 行为不变。
- [ ] 更新 Window Rename E2E 场景覆盖 host 与 viewer 双击。
- [ ] 运行受影响测试与 macOS 构建验证。

## Decisions

- 继续使用 `WindowRenamingModifier` 和 `TextEntryPresentation`，不复制 alert/sheet 状态。
- 双击只绑定 terminal window 标签的主按钮，不覆盖 close、split 或其他标签。
- 使用 macOS `NSApp.currentEvent?.clickCount`，与 session 行已有双击实现保持一致。
- 不增加 relay 或 tmux 命令；保存仍走现有 `onRenameWindow`。

## Blockers

- None.

## Verification

- Pending.
