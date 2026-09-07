# Build notes

TapForge 1.0 uses:

- application ID: `com.tapforge`
- version name: `1.0`
- version code: `15`
- compile SDK: `35`
- target SDK: `35`
- minimum SDK: `24`
- Android Gradle Plugin: `8.7.3`
- Java source/target: `17`

There are no app runtime dependencies declared in `app/build.gradle`.

A distribution service such as F-Droid should build `assembleRelease` from the tagged source and apply its own signing key. The upstream repository should not commit private signing material.
