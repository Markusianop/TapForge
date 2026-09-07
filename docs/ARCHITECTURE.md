# TapForge architecture

TapForge 1.0 deliberately uses Android framework APIs and Java/XML views rather than introducing a large UI/runtime dependency stack immediately before its first public release.

## Layers

### NFC protocol / data

- `NdefFactory` builds NDEF records and messages.
- `NdefHostApduService` exposes Type 4 NDEF through Android HCE.
- `CustomHostApduService` exposes the user-configured custom AID/APDU response mode.
- `TagInspector` and `NfcAnalysis` decode and summarize physical tags.
- `VCardBuilder` / `VCardParser` handle contact interchange.

### Local persistence

- `NdefProfileStore` and `TapForgeProfiles` store active and reusable profiles.
- `NfcHistory` stores local operation history.
- `NdefUndoStore` stores the single rollback snapshot for Undo Last Write.

### UI / workflows

Activities are intentionally workflow-oriented: Send, Receive, Tools, Write, Studio, Library, Batch, Compare, Compatibility and APDU Lab. `Navigation`, `Motion`, `ThemeHelper`, `LocaleHelper` and `UiAdaptation` centralize cross-screen behavior.

## Design constraints

1. NFC actions are explicit and local-first.
2. HCE and physical-tag flows share NDEF creation/parsing code where practical.
3. Read-only/destructive operations must remain clearly confirmed.
4. TapForge does not represent protected payment, transit or access credentials as generally cloneable.
5. UI motion respects the Reduce Motion preference.
6. Avoid network dependencies unless a future feature has a compelling, documented reason.

## Future refactoring

A future major release can migrate presentation code to Compose/MVVM incrementally. The protocol/data classes above should remain independent enough that the NFC engine does not need to be rewritten at the same time as the UI.
