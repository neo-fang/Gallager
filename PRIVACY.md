# Privacy baseline

CtrlX terminal payloads are end-to-end encrypted between paired clients. A Relay
can still process operational metadata including IP addresses, device IDs and
names, pairing relationships and timestamps, APNs tokens, connection events and
abuse/security logs.

The community source does not define a hosted CtrlX production service or its
retention policy. Every operator must document actual collection, retention,
deletion, backup and incident-response practices before inviting users.

CtrlX does not require telemetry, payment, or account services for self-hosting.
Optional monitoring must remain authenticated and must not include decrypted
terminal content or secrets.
