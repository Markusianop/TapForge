# TapForge

TapForge is a free and open-source NFC toolkit for Android. It combines everyday NDEF tag workflows with phone-side HCE, diagnostics and lower-level ISO-DEP tools in one local-first app.

**Public release:** 1.0  
**Package:** `com.tapforge`  
**License:** GPL-3.0-or-later

## What TapForge can do

### Send and receive
- Emulate NFC Forum Type 4 NDEF from the phone with Android HCE.
- Share URL, text, email, phone, contacts/vCard, MIME and saved multi-record NDEF profiles.
- Receive standard Android NDEF and fall back to direct ISO-DEP / Type 4 reads where available.
- Custom HCE/APDU mode with user-defined AID and response data.

### Physical tags
- Read tag UID, technologies, NDEF type, capacity, writable state, utilization and decoded records.
- Write URL, text, email, phone, SMS, location, contact, Wi-Fi/WPS, Android app records, MIME, NFC Forum external types and raw NDEF.
- Clone **NDEF content** between compatible tags.
- Erase NDEF, initialize NDEF-formatable tags and request permanent read-only mode where Android/tag hardware supports it.
- Decode common records, including Wi-Fi/WSC data.

### Create and reuse
- **NDEF Studio** for true multi-record NDEF messages.
- **Profile Library** for local reusable profiles and JSON import/export.
- Activate saved multi-record profiles as the phone's HCE payload.
- **Batch Write** with duplicate UID protection and optional read-back verification.
- Optional verification after normal writes.

### Analyze and recover
- Compare two tags, including SHA-256 fingerprints of their NDEF payloads.
- Non-destructive compatibility/preflight check before writing.
- APDU Lab for user-entered ISO-DEP command sequences and raw responses.
- Undo the most recent NDEF overwrite by restoring the locally captured previous message to the same UID.
- Local NFC operation history.

TapForge does **not** claim to clone protected payment, transit, access-control or cryptographic credentials. Its clone/backup features operate on readable NDEF data.

## Privacy

TapForge is local-first:

- no `INTERNET` permission;
- no analytics;
- no advertising SDK;
- no account;
- no cloud backend;
- contact access is optional and requested only when importing a contact;
- history, profiles and rollback data stay on the device unless the user explicitly exports a file.

See [PRIVACY.md](PRIVACY.md).

## Build from source

Requirements:

- JDK 17
- Android SDK Platform 35
- Android Build Tools 34.0.0 or compatible tools selected by AGP
- Gradle 8.9

```bash
echo 'sdk.dir=/opt/android-sdk' > local.properties
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH="$JAVA_HOME/bin:$PATH"
gradle clean assembleDebug
```

For the unsigned release artifact used by reproducible/distribution builds:

```bash
gradle clean assembleRelease
```

The project intentionally has no runtime third-party dependencies.

## F-Droid

The repository contains Fastlane/F-Droid-compatible store metadata and an example `fdroiddata` recipe under `fdroid/`.

Before submitting to the official F-Droid repository, publish this source tree in a public Git repository, tag the release as `v1.0`, and replace the placeholder repository URLs in `fdroid/com.tapforge.yml.example` with the real repository URL.

F-Droid should build and sign its own APK from source. Do not submit a privately signed APK as a substitute for the reproducible source build.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Security-related reports are covered by [SECURITY.md](SECURITY.md).
