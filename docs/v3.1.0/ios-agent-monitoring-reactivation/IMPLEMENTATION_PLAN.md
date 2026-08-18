# CtrlX 3.1.0 Stage 6：iOS Agent 监控自恢复

## 问题

`Background Agent Monitoring` 开关同时表示了用户意图和当前
`BGContinuedProcessingTask` 租约。两小时预算用尽或系统取消租约时，
运行状态变为 `inactive`，根视图随即把开关回写为关闭。之后的 Agent
输入因设置门禁而不再建立新租约。

## 设计

1. 开关只表示并持久化用户是否需要 Agent 后台监控。
2. `MonitoringStatus` 只表示当前进程的系统租约状态。租约失效不修改开关。
3. 开启开关或提交非空 Agent prompt/终端输入时，若没有租约则立即
   建立新租约。这些都是明确的前台用户操作。
4. App 启动、scene 回到前台和后台计时器不提交新租约。Apple 要求
   `BGContinuedProcessingTaskRequest` 由用户操作触发，不做伪后台续租。
5. 租约到期或预算用尽时清理 pane phase、终端输入累计和连接节流
   状态，避免新租约继承过期的 `working` 计数。
6. 用户关闭开关时始终执行完整清理，不依赖当前租约状态。
7. 设置页在开关已启用但租约不活跃时显示 `Ready`；下一次 Agent
   输入会自动恢复。

## 系统边界

- iOS 可在任意时刻取消 continued-processing task。系统已开始显示失败的
  旧卡片无法由已失去执行权的 App 改写。
- 后台中不能无用户操作无限创建新租约；真正的远程唤醒仍需 APNs。
- 开关保持开启表示“下次用户输入时自恢复”，不声称当前始终拥有
  后台执行权。

## 验收标准

- 租约到期或系统取消后，开关保持开启，状态显示 `Ready`。
- 下一次有效 Agent 输入会创建且仅创建一个新租约。
- App 重启后保留开关偏好，但不在启动或回前台时自动提交。
- 关闭开关会清理活跃租约和所有监控上下文。
- Agent 监控定向测试、完整 Swift 测试和 iPhoneOS 构建通过。
