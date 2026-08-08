# Stage 27 Code Review Report

## Scope

- iOS session 内窗口选择、切换与 session 缺失判断。
- Host 断线、重连和密集全量状态刷新时的 SwiftUI task 生命周期。
- App 内主动关闭 session 后的导航行为。

## Findings

### Critical / High / Medium

- None.

### Low

- 真正由其他 Viewer 删除 session 时，页面最多延迟 2 秒返回列表。这是过滤瞬时空快照所需的
  有界确认窗口；App 内主动关闭 session 成功后仍立即返回。

## Correctness checks

- 初次进入且尚无状态时不会把空列表误判成 session 消失。
- 当前 window 短暂缺失时优先切换到 Host 标记的 active window，否则选择排序后的首个 window。
- 窗口候选、Host 连接状态或首帧状态变化都会取消旧确认任务并重新协调，旧连接任务不能延迟退出页面。
- 只有 Host 已连接、已收到状态且连续 2 秒仍无 session 时才返回列表。
- 未修改 relay 协议、tmux 命令、键盘输入或 terminal stream。
- 6 项聚焦测试、1658 项完整测试和 iOS generic device build 通过。

## Assessment

Approved for device acceptance. 实现只增加一个纯状态决策和一个可取消的有界确认任务，
没有引入缓存、额外状态机或协议分支。
