# F-Droid submission notes

This directory contains the upstream metadata draft for TapForge.

Source repository: https://github.com/Markusianop/TapForge

For an official F-Droid submission:

1. Tag the exact public source release as `v1.0`.
2. Verify a clean `assembleRelease` build with JDK 17 and Android SDK 35.
3. Validate `com.tapforge.yml` against the current `fdroiddata` metadata schema.
4. Add representative screenshots to the Fastlane metadata folders.
5. Submit the metadata to the official F-Droid `fdroiddata` repository using the current contribution process.

No signing key is committed. F-Droid is expected to build and sign its distributed APK from source.
