# Stage 4 TODO

## Stage Status

- **Status**: ✅ Completed
- **Progress**: 5/5 tasks
- **Dependencies**: Stage 3 ✅

## Tasks

- [x] 定位 TIFF 原始数据与 Relay 700 KiB 上限的冲突。
- [x] 实现 Relay 图片规范化与超限压缩。
- [x] 将图片准备移出主线程并接入上传状态。
- [x] 增加图片编码边界回归测试。
- [x] 构建安装并完成真实剪贴板图片粘贴验收。

## Decisions

- 不提高 Relay 的 WebSocket frame 上限。
- 不为单张图片引入分片上传协议。
- 不改变 Finder 文件 drop 的原始文件语义和大小限制。
