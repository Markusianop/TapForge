# Contributing to TapForge

Contributions are welcome.

## Development setup

TapForge is a plain Android/Java project with no runtime third-party libraries.

Recommended toolchain:

- JDK 17
- Android SDK 35
- Gradle 8.9

Build with:

```bash
gradle clean assembleDebug
```

## Before opening a change

Please check that:

1. the app still builds without adding `INTERNET` unless a future feature has a strong, documented reason;
2. NFC operations remain explicit and user-initiated;
3. destructive operations such as permanent read-only mode keep a clear confirmation/warning;
4. protected payment/access credentials are not represented as generally cloneable;
5. new strings have a sensible English fallback;
6. UI motion respects the Reduce Motion preference;
7. tag-writing changes are tested against both writable `Ndef` and, where applicable, `NdefFormatable` tags.

## Code style

Prefer Android platform APIs and small self-contained Java classes. Avoid adding dependencies for functionality that can reasonably be implemented with the framework.

## Licensing

By contributing, you agree that your contribution may be distributed under GPL-3.0-or-later, the license used by TapForge.
