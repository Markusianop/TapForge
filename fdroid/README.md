# F-Droid submission files

`com.tapforge.yml` is the upstream metadata draft for the first public release.

Before submitting to the official `fdroiddata` repository:

1. ensure GitHub Actions builds the exact `v1.0` tag;
2. confirm `versionName 1.0` and `versionCode 15` in `app/build.gradle`;
3. run `fdroid lint com.tapforge` against the current fdroidserver/fdroiddata schema;
4. submit the metadata through the normal F-Droid inclusion process.

F-Droid builds and signs its own APK from source. Do not commit private signing keys.
