# CtrlX 3.1.0 Stage 7：iOS Agent 监控前台续期

## 问题

当前 app-wide `BGContinuedProcessingTask` 使用有限的两小时进度预算。即使用户
持续打开 CtrlX，预算仍从最初创建租约时开始消耗；任务到期后只能等下一次
有效 Agent 输入建立新租约。

## 设计

1. 当前租约记录可变的进度上限，初始仍为 720 个十秒单位。
2. 只在真实的 `background → active` scene 转换时续期；首次启动、
   `inactive → active` 和 SwiftUI 视图重建不续期。
3. 续期把上限推进到“当前完成单位 + 720”，即从回到前台时再保留约两小时预算。
4. 复用当前 `BGContinuedProcessingTask` 和系统卡片，只更新进度总量，不提交
   新 request，不重置任务开始时间、pane 状态或连接状态。
5. 当前租约已经失效时不在 scene callback 中创建新租约；仍由下一次前台
   Agent 输入按 Stage 6 规则恢复。
6. iOS 仍可因系统资源策略提前终止任务；续期只延长 CtrlX 自己的有限预算，
   不承诺永久后台执行。

## 验收标准

- 活跃租约从后台回到前台后，剩余预算恢复为约两小时。
- 同一前台周期内的重复 active 通知不重复续期。
- 首次启动和无租约回前台不创建系统任务或卡片。
- 续期不改变当前 Agent 卡片内容和 pane phase。
- Agent 监控定向测试、完整 Swift 测试和 iPhoneOS 构建通过。
