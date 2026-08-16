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

- 将任务标识改为每个 Host + pane 在单次 App 生命周期内复用，避免每轮 prompt 都永久增加一个系统 handler。
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
