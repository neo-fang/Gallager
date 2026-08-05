# Gallager 2.7 iOS 自托管首次配对实施计划

## 状态

- **状态**：✅ 已完成
- **进度**：1/1 stage

## 问题

iOS 在未配对时默认使用 `wss://relay.gallager.app`，但首次配对页不暴露
`IOSSettings.externalServerURL`。六位配对码不包含 Relay 地址，因此 TestFlight
客户端无法首次连接自托管 Relay。

## Stage 1：首次配对页的 Relay 配置

1. 在 `PairingView` 增加原生 `TextField`，直接绑定现有
   `IOSSettings.externalServerURL`，不增加第二份状态或硬编码私有域名。
2. 配对前验证 URL 必须为 `ws://` 或 `wss://`，及时告知用户而不发出无效请求。
3. 保持六位码自动提交、粘贴、键盘焦点和配对成功后多 host 行为不变。
4. 用聚焦单元测试覆盖 Relay URL 规范化/验证，再构建并安装到真机。

## 验收标准

- 全新安装、未配对状态下可见并修改 Server URL。
- 值直接持久化到 `IOSSettings`，重启 App 后不丢失。
- 仅接受 `ws`/`wss` Relay URL，配对 API 仍通过现有 `httpURL` 转换访问。
- 真机能通过自托管 Relay 配对，并看到默认 tmux socket 中的 session。
- 未配置 APNs 时不影响前台 WebSocket 与终端功能，不宣称后台推送通过。
