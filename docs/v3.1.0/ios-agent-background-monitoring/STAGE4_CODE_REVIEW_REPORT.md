# Stage 4 Code Review Report

## Review scope

- iOS 26 `BGContinuedProcessingTask` 封装和系统生命周期
- Agent prompt 识别、状态转换和任务结束条件
- 设置持久化、可用性提示及关闭开关后的清理
- Host 断连、系统到期和系统拒绝任务时的降级路径

## Findings

- P1：无
- P2：无
- P3：无

## Resolved during review

- 真机崩溃报告显示 launch handler 在 BGTaskScheduler 私有队列触发了 Swift 6 actor 隔离检查。显式指定 main queue，使 handler 与 `@MainActor` runtime 的执行器一致。
- 每轮用户提交使用唯一动态任务标识，避免系统拒绝重复提交已经完成的 continued-processing request。
- 将未知总工作量声明为 indeterminate progress，并用限频后的真实 terminal data chunk 增加活动单位，避免 Agent 持续工作但固定停在 1/2 时被系统判定为 stalled。
- 系统到期时明确调用 `setTaskCompleted(success: false)`，避免留下未完成的后台任务。
- 修正文档和代码注释，使终端键盘提交路径与结构化 Agent response 的边界一致。

## Maintainer assessment

- 开关默认关闭，升级不会改变现有后台行为。
- 任务只覆盖用户主动提交的一轮有限 Agent 工作；不会为 idle socket 创建无限保活。
- 使用 `.fail` 策略。系统无法立即接受任务时安全降级为现有前台生命周期。
- 完成、等待输入、Host 断连、关闭开关和系统到期均有明确结束路径。
- 复用既有 Agent 状态和本地通知链路，不改变 Host、Relay、macOS 或传输协议。

## Verification

- `swift test --skip-update --filter AgentBackgroundMonitoringPolicyTests`：6 tests passed
- iPhoneOS generic destination build with code signing disabled：passed
- `git diff --check`：passed

## Decision

Approved for physical-iPhone acceptance. Stage 4 remains unmerged until the user verifies the enabled and disabled paths on a real device.
