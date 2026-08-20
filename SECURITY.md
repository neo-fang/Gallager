# Security policy

CtrlX is an end-to-end encrypted terminal control product. Vulnerabilities that
expose terminal plaintext, pairing secrets, private keys, or command input have
the highest priority.

## Reporting

Use private vulnerability reporting in the CtrlX GitHub repository once it is
enabled. Until that public repository is active, do not publish exploit details
in an issue; contact the maintainer listed in `NOTICE.md`.

## Supported versions

Only the latest CtrlX release receives security fixes.

## Scope

- Mac/iOS E2EE, pairing, Keychain and local socket behavior
- Relay routing, authorization, rate/size boundaries and APNs handling
- Release, update and self-hosting scripts shipped in this repository

The Relay sees routing metadata and encrypted frames, not terminal plaintext.
Operator-specific firewall or account hygiene is outside the application scope,
but unsafe defaults in the committed deployment recipes remain in scope.
