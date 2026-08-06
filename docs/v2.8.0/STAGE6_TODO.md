# Stage 6 TODO：Session 名称展示优先级

## Stage Status

- **Status**: 🟡 In Progress
- **Progress**: 2/10 tasks
- **Dependencies**: Stage 5 ✅

## Tasks

- [x] 确认 rename 成功但 Description 遮住 session name 的 Mac/iOS 展示根因。
- [x] 确定 session name 为默认主身份，Description 保持独立副信息。
- [ ] 调整 Mac agent 和 terminal 默认 Sidebar Fields。
- [ ] 仅迁移完全等于旧默认值的已保存 Mac 配置。
- [ ] 调整 iOS agent 和 terminal 会话行的主副标题。
- [ ] 调整 iOS 会话详情页标题。
- [ ] 调整 Mac 菜单栏会话行标题。
- [ ] 增加默认值、迁移与展示规则聚焦测试。
- [ ] 运行 Swift package 测试及 macOS/iOS 构建。
- [ ] Mac 与 iPhone 真机验收 rename + Description 并存展示。

## Decisions

- Rename 始终修改 tmux `session_name`；Description 不随 rename 改写或清空。
- Mac 仍保留 Sidebar Fields 自定义；只升级旧默认预设，不强制修改自定义配置。
- iOS 没有 Sidebar Fields 配置，因此固定使用 session name 主标题。
- 不增加 wire 字段或 relay 逻辑；`sessionName` 和 Description 已同时存在现有快照中。

## Blockers

- 无。
