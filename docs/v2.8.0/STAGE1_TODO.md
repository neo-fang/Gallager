# Stage 1 TODO

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 0/6 tasks
- **Dependencies**: None

## Tasks

- [ ] 增加 session 名验证和 tmux `rename-session` 服务。
- [ ] 增加 relay 命令并在 host 端刷新、推送状态。
- [ ] 修复 pane stream/control client 在 session 改名后的旧名称引用。
- [ ] 增加 macOS/iOS 菜单重命名及 macOS 双击输入交互。
- [ ] 迁移本地与远程 viewer 的选择及 session-scoped UI 状态。
- [ ] 完成单元测试、macOS/iOS 构建和交互验收。

## Decisions

- Description 保持独立元数据，不作为 session rename 的替代品。
- 不引入新的持久化 session UUID；本阶段在现有 tmux session-name 身份模型上做
  原子迁移，避免无必要的协议和存储重构。
- 双击仅用于 macOS；iOS 保持单击导航、长按菜单编辑，避免双击与
  `NavigationLink` 的首击导航冲突。

## Validation

- 待完成。
