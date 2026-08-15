# Stage 5 TODO

## Stage Status

- **Status**: ✅ Completed
- **Progress**: 5/5 tasks
- **Dependencies**: Stage 4 ✅

## Tasks

- [x] 用 Relay/Nginx 日志确认图片粘贴触发 WebSocket 断线重连。
- [x] 建立共享的 WebSocket frame 与文件原始数据预算。
- [x] 增加最终端到端加密帧大小回归测试。
- [x] 构建安装并用当前剪贴板图片验证 session 不重载。
- [x] 更新并校验 macOS DMG。

## Decisions

- 不提高 Relay 的 1 MiB WebSocket frame 上限。
- 不为单张图片引入分片协议和重组状态。
- 预算必须覆盖两层 Base64、加密和 JSON 开销，不能再由单层 Base64 估算。
