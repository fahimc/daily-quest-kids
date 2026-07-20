# Release Checklist

Release blockers:

- [x] All 17 implementation phases have automation or documented blockers.
- [x] Exactly 1,825 Season One candidate puzzle records exist and validate
  structurally.
- [ ] Human content review is complete and documented.
- [x] No internet permission or tracking dependency is present.
- [x] Engine-level save/restore simulation passes for all 1,825 candidate
  puzzles.
- [x] Share rendering leak tests pass.
- [x] Full-season simulation passes.
- [ ] Manual accessibility review passes.
- [ ] VRT baselines are approved for required screens and sizes.
- [ ] Release APK and AAB build from a private signing configuration.
- [ ] Signing uses local or CI-provided private keys only.

Commands:

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug ktlintCheck detekt assembleDebug
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat assembleRelease bundleRelease
```

Current prerelease artifacts:

- `release-artifacts/daily-quest-kids-v0.9.0-debug.apk`
- `release-artifacts/daily-quest-kids-v0.9.0-release-unsigned.apk`
- `release-artifacts/daily-quest-kids-v0.9.0-release.aab`
