# TapForge

TapForge is a free and open-source NFC toolkit for Android. It combines everyday NDEF workflows with phone-side HCE, physical-tag tools, diagnostics and ISO-DEP experimentation in one local-first app.

**Public release:** 1.0  
**Package:** `com.tapforge`  
**License:** GPL-3.0-or-later

## Features

- **Send / HCE:** emulate NFC Forum Type 4 NDEF from the phone.
- **Receive:** read standard NDEF and compatible ISO-DEP / Type 4 sources.
- **Physical tags:** inspect, write, clone NDEF, erase, initialize and request read-only mode when supported.
- **NDEF Studio:** compose true multi-record NDEF messages.
- **Profile Library:** save, import/export and reuse NDEF profiles; saved profiles can be activated for HCE.
- **Batch Write:** duplicate-UID protection and optional read-back verification.
- **Analyze:** Compare Tags, compatibility/preflight checks and SHA-256 NDEF fingerprints.
- **APDU Lab:** send user-entered ISO-DEP commands and inspect raw responses.
- **Recovery:** Undo Last Write can restore the previous readable NDEF to the same tag UID.
- **History:** local NFC operation history.
- URL, text, email, phone, SMS, location, contacts/vCard, Wi-Fi/WPS, Android app records, MIME, external types and raw NDEF.

TapForge clones **readable NDEF content only**. It does not claim to clone protected payment, transit or access-control credentials.

## Privacy

TapForge is local-first:

- no `INTERNET` permission;
- no analytics;
- no advertising SDK;
- no account or cloud backend;
- optional `READ_CONTACTS` only when importing a contact;
- history, profiles and rollback data remain on-device unless explicitly exported.

See [PRIVACY.md](PRIVACY.md).

## Build

Recommended toolchain:

- JDK 17
- Android SDK Platform 35
- Android Build Tools 34.0.0
- Gradle 8.9

```bash
echo 'sdk.dir=/opt/android-sdk' > local.properties
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH="$JAVA_HOME/bin:$PATH"
gradle clean assembleDebug
```

Release build:

```bash
./scripts/build-release.sh
```

If these environment variables are provided, the release variant is signed with that keystore:

```text
TAPFORGE_KEYSTORE
TAPFORGE_STORE_PASSWORD
TAPFORGE_KEY_ALIAS
TAPFORGE_KEY_PASSWORD
```

Never commit the keystore or passwords.

## F-Droid

Upstream metadata lives at [`fdroid/com.tapforge.yml`](fdroid/com.tapforge.yml), and store metadata is under [`fastlane/metadata/android`](fastlane/metadata/android).

Source: https://github.com/Markusianop/TapForge

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [NFC compatibility](docs/NFC_COMPATIBILITY.md)
- [Privacy](PRIVACY.md)
- [Security](SECURITY.md)
- [Contributing](CONTRIBUTING.md)
- [Changelog](CHANGELOG.md)
