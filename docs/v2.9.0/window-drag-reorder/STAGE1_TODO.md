# Window 拖拽排序修复 TODO

## Stage Status

- **Status**：🟡 Implementation Complete / Pending UI Acceptance
- **Progress**：6/7 tasks
- **Dependencies**：`develop/v2.9.0` ✅

## Tasks

- [x] 复现并确认落点、可变 ID、base-index 和并发问题
- [x] 贯通 tmux 稳定 Window ID
- [x] 使用无损 swap 算法重排 tmux Window
- [x] 统一本地/远程拖拽语义并实现 pending/回滚
- [x] 迁移异地触发的旧 target 状态并兼容 linked Window
- [x] 增加模型、算法和隔离 tmux 回归测试
- [x] 完成代码审查、macOS 构建和签名校验
- [ ] 覆盖安装后完成本地/远程鼠标拖拽验收

## Blockers

- 当前无代码阻塞；应用内验收需要覆盖正在运行的 Gallager，留给用户确认安装时执行。

## Verification

- 相关测试：61 项通过，覆盖稳定身份、协议兼容、落点、回滚、linked Window、布局映射与真实 tmux 重排。
- 真实 tmux 测试使用独立 socket，确认重排后 Window/Panes 无丢失且索引槽位保持不变。
- macOS `ClaudeSpyServer` Debug 工程构建通过。
- 构建产物 `codesign --verify --deep --strict` 通过。
- `git diff --check` 通过。
- SwiftFormat 全文件 lint 仍命中仓库既有格式债务；未做无关的大范围机械改写。
