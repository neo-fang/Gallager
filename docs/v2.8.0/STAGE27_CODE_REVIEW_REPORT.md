# Stage 27 Code Review Report

## Scope

- iOS session 内窗口选择、切换与 session 缺失判断。
- Host 断线、重连和密集全量状态刷新时的 SwiftUI task 生命周期。
- App 内主动关闭 session 后的导航行为。

## Findings

### Critical / High / Medium

- **High**：第一版只处理了 `WindowLayoutView` 的空快照路径，遗漏了
  `LiveTerminalView` 在任意 pane `streamEnd` 时调用父级 `dismiss()`。真机切换窗口仍可
  返回 session 列表；第一版不得视为通过。

### Low

- 外部关闭 session 后不再自动返回列表，需要用户使用系统返回按钮。空快照没有可依赖的
  延迟上界，保留页面比基于定时器误退出更可靠；App 内主动关闭成功后仍立即返回。

## Correctness checks

- 初次进入且尚无状态时不会把空列表误判成 session 消失。
- 当前 window 短暂缺失时优先切换到 Host 标记的 active window，否则选择排序后的首个 window。
- 窗口候选、Host 连接状态或首帧状态变化都会取消旧确认任务并重新协调，旧连接任务不能延迟退出页面。
- 空快照和 pane `streamEnd` 都不得改变 session 导航层级。
- 未修改 relay 协议、tmux 命令、键盘输入或 terminal stream。
- 6 项聚焦测试、1658 项完整测试和 iOS generic device build 通过。

## Assessment

Changes required. 第一版真机验收失败；完成隐式导航删除和流恢复验证后重新评审。
