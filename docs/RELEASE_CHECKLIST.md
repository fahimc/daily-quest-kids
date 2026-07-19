# Release Checklist

Release blockers:

- All 17 implementation phases complete.
- Exactly 1,825 production puzzles exist and validate.
- Human content review is complete and documented.
- No internet permission or tracking dependency is present.
- Progress persistence survives process death and upgrades.
- Share rendering leak tests pass.
- Full-season simulation passes.
- Accessibility review passes.
- VRT baselines are approved for required screens and sizes.
- Release APK and AAB build.
- Signing uses local or CI-provided private keys only.

Commands:

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug ktlintCheck detekt assembleDebug
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat assembleRelease bundleRelease
```
