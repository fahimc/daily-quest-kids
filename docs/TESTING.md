# Testing

Testing layers:

- JVM unit tests for date, streak, validators, share safety and puzzle engines.
- Android instrumented tests for Activity startup and Compose UI behaviour.
- Android Lint for platform and manifest checks.
- Ktlint for formatting.
- Detekt for Kotlin static analysis.
- GitHub Actions template at `docs/CI_WORKFLOW_TEMPLATE.yml`; moving it to
  `.github/workflows/android.yml` requires GitHub token `workflow` scope.
- Screenshot/VRT tests are planned as deterministic instrumented tests with
  fixed locale, clock, animation scale, font scale and device sizes.

Local commands:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat --no-daemon --no-parallel "-Dorg.gradle.workers.max=1" lintDebug test ktlintCheck detekt assembleDebug compileDebugAndroidTestKotlin
```

Device/emulator command:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Latest verification on 2026-07-19:

- Local non-device gate: passed.
- Phase 1-5 focused gate: passed with
  `$env:GRADLE_USER_HOME = "D:\gradle-user-home"` because C: had very little
  free space.
- Focused command:
  `.\gradlew.bat --no-daemon --no-parallel --console=plain "-Dorg.gradle.workers.max=1" :puzzle-validator:test :app:testDebugUnitTest ktlintCheck detekt :app:compileDebugAndroidTestKotlin`
- Phase 6 Wordly focused gate: passed.
- Phase 6 command:
  `.\gradlew.bat --no-daemon --no-parallel --console=plain "-Dorg.gradle.workers.max=1" :puzzle-engine:test :puzzle-validator:test :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin`
- Static analysis command:
  `.\gradlew.bat --no-daemon --no-parallel --console=plain "-Dorg.gradle.workers.max=1" ktlintCheck detekt`
- Connected Android tests: blocked by `No connected devices!`.
- Screenshot smoke and Wordly visual-state tests compile into the Android test
  APK but have not executed on hardware/emulator yet.

Coverage target:

- At least 90% for core domain logic.
- Full branch coverage where practical for validators.
- Full structural validation of the final 1,825-puzzle pack.
