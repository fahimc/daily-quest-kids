# Installation

Daily Quest Kids currently publishes prerelease Android artifacts for local
testing.

## Debug APK

Install the debug APK with:

```powershell
adb install -r release-artifacts\daily-quest-kids-v0.9.0-debug.apk
```

## Release APK

The release APK is built for verification, but it is unsigned unless a local or
CI signing configuration is supplied.

```powershell
adb install -r release-artifacts\daily-quest-kids-v0.9.0-release-unsigned.apk
```

## Release AAB

The release bundle is generated for store-readiness checks:

```text
release-artifacts/daily-quest-kids-v0.9.0-release.aab
```

Private signing keys must not be committed. Configure signing through local
Gradle properties or CI secrets before production distribution.
