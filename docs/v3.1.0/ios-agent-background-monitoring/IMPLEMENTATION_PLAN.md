# CtrlX 3.1.0 Stage 4：iOS Agent 后台监控

## 问题

CtrlX 当前依赖前台 Relay 连接接收 Agent 状态并触发本地通知。App 进入后台后，
系统只给现有短时后台任务有限的执行窗口，因此刚离开 App 时偶尔还能收到通知，
但不能可靠覆盖一次较长的 Agent 运行。

## 设计

1. Settings 的 Agent Input 区域增加 `Background Agent Monitoring` 开关，默认关闭。
2. 仅在 iOS 26 及以上启用 `BGContinuedProcessingTask`；旧系统保留现状。
3. 仅当用户从 iOS 提交非空 Agent prompt 时创建后台监控任务：
   - `.prompt`
   - `.replyAfterStop`
   - Agent idle/done 状态下通过终端键盘提交的非空输入行
   - 系统请求在 Enter/Send 的前台事件内同步提交，不等待 Relay 回执，也不推迟到异步任务
4. 使用 `.queue` 提交策略。系统无法立即启动时允许稍后接管监控；Agent 提前结束时取消尚未启动的请求。
5. 每个 Host + pane 同时只保留一个监控任务；再次提交会替换旧任务。
6. 用真实 Agent 状态驱动两小时上限的有限监控进度：
   - prompt 已提交：等待 Agent 启动；
   - `.working`：Agent 正在工作；
   - 活跃监控期间：每 10 秒完成一个 monitoring activity unit，总计 720 units；
   - 完成、等待用户输入、失败、Host 断开、用户关闭开关或系统取消：结束任务。
   - 系统卡片标题显示 `session name · window name`，副标题显示 status；旧 Host 缺少 window name 时标题退化为 session name。
7. 继续复用现有 `onAgentNotification` 本地通知链路，不额外发送重复通知。
8. `BGTaskScheduler` 封装为依赖，状态转换保持为平台无关纯逻辑，以便单元测试。

## 生命周期

```text
iOS 提交 prompt 成功
        │
        ▼
  waitingForAgent ── working ── completed / waitingForInput
        │                │
        └──── host disconnect / setting off / system cancel ────┘
```

任务具有明确起点和终点。Agent 没有可预知的完成百分比，因此圆环表示两小时
后台监控预算的消耗；活动单位不表示 Agent 工作完成度。CtrlX 不循环续期，也不试图
在没有活动 Agent 任务时维持 Relay 常驻。

## 系统行为与限制

- 系统会在锁屏和系统界面显示该 continued-processing 活动，并允许用户取消。
- CPU 和网络访问使用基础能力，不依赖 APNs entitlement。
- 任务仍受系统调度和资源策略约束；开关开启不等于永久后台运行。
- 此功能不能绕过 Personal Team 描述文件有效期，也不能替代正式 APNs 推送。

## 非目标

- 不实现无限后台保活、定时重启任务或静默自唤醒。
- 不改变 Relay 协议、Mac Host 或既有本地通知格式。
- 不为普通 shell 输入、快捷键、审批按钮或空 prompt 创建后台任务。
- 不为 iOS 26 以下系统模拟 continued processing。

## 验收标准

- 开关默认关闭，并能持久化。
- iOS 26 开启后，成功发送 Agent prompt 会建立可见的 continued-processing 活动。
- Agent 开始、完成或等待用户输入时，进度和结束状态正确。
- 关闭开关、Host 断开、系统取消时不会留下后台任务。
- 开关关闭及 iOS 26 以下行为与当前版本一致。
- 状态转换单元测试、iPhoneOS 构建通过，并完成 iPhone 真机验收。
