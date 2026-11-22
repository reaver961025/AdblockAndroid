# Update changelog — fork `reaver961025/AdblockAndroid`

Branch: `update/modernize-toolchain`
Date: 2026-11-22

This is the first pass of modernization of the original `Edsuns/AdblockAndroid`
(which was last touched in early 2021 and used Gradle 6.5 / AGP 4.1.2 /
Kotlin 1.4.21 / `compileSdk` 30 / `jcenter()`).

## Toolchain

| Item | Before | After |
| --- | --- | --- |
| Gradle wrapper | 6.5 | **8.7** |
| Android Gradle Plugin | 4.1.2 | **8.5.2** |
| Kotlin | 1.4.21 | **1.9.24** |
| `compileSdk` / `targetSdk` | 30 | **34** |
| `minSdk` | 23 | 23 (unchanged) |
| `buildToolsVersion` | `"29.0.3"` (pinned) | removed — AGP 8 selects its own |
| CMake | pinned `"3.10.2"` (no longer downloadable) | unpinned — AGP 8 uses its bundled/default CMake |

AGP 8 disables `BuildConfig` generation by default for libraries. Because the
source references `BuildConfig.DEBUG` (`ad-filter/.../ScriptInjection.kt` and
`app/.../App.kt`), `buildFeatures { buildConfig = true }` was added explicitly
to the `ad-filter` and `app` modules.

The deprecated `compileSdkVersion` / `minSdkVersion` / `targetSdkVersion` calls
were renamed to the AGP 8 DSL `compileSdk` / `minSdk` / `targetSdk`.

## Repositories

`jcenter()` (shut down, read-only and missing artifacts) was removed from both
the `buildscript` and `allprojects` repository lists in the root `build.gradle`.
Replaced with `mavenCentral()`. `google()` and the `jitpack.io` maven repo are
kept.

## Dependencies

Removed (unused in source — 0 `org.jetbrains.anko` imports found):
- `org.jetbrains.anko:anko-commons:0.10.4`
- `org.jetbrains.anko:anko-design:0.10.4`

Bumped:

| Dependency | Before | After |
| --- | --- | --- |
| `com.jakewharton.timber:timber` | 4.7.1 | **5.0.1** |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` / `-test` | 1.3.7 | **1.8.0** |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.0.1 | **1.6.3** |
| `androidx.core:core-ktx` | 1.3.2 | **1.13.1** |
| `androidx.appcompat:appcompat` | 1.2.0 | **1.7.0** |
| `androidx.constraintlayout:constraintlayout` | 2.0.4 | **2.1.4** |
| `androidx.activity:activity-ktx` | 1.2.0 | **1.9.2** |
| `androidx.fragment:fragment-ktx` | 1.3.0 | **1.8.4** |
| `androidx.work:work-runtime-ktx` | 2.4.0 | **2.9.1** |
| `com.google.android.material:material` | 1.2.1 | **1.12.0** |
| `junit:junit` | 4.13.1 | **4.13.2** |
| `androidx.test.ext:junit` | 1.1.2 | **1.2.1** |
| `androidx.test.espresso:espresso-core` | 3.3.0 | **3.6.1** |

Kept as-is (only available versions, hosted on jitpack):
- `com.github.Edsuns:HttpRequest:0.2`
- `com.github.Edsuns.Mezzanine:mezzanine:1.2.1` (+ `mezzanine-compiler` kapt)
- `com.github.Edsuns:SmoothProgressAnimator:1.0`

## Versioning

App and library modules bumped from `versionCode 1` / `versionName "1.0"` to
`versionCode 2` / `versionName "2.0.0"` to mark the update.

## README

- JitPack badge and dependency coordinate updated to the fork
  `com.github.reaver961025.AdblockAndroid:ad-filter:2.0.0`.
- Added an update notice pointing to this file and noting the JDK 17+ /
  Android Studio + NDK + CMake requirement.

## Verification status

- The Gradle wrapper was upgraded to 8.7 and confirmed to launch under JDK 21
  (`./gradlew --version`) — i.e. the wrapper itself is valid.
- **A full build could NOT be run in the sandbox** because the Android SDK,
  NDK and CMake are not installed here. The configuration changes are
  syntactically coherent for AGP 8.5.2, but compilation must be verified on a
  machine with Android Studio (recent Canary/Beta or stable 2024+), JDK 17+,
  and the NDK + CMake components.

## Remaining steps (to do)

1. Open the project in Android Studio and let it sync. Install the NDK + CMake
   via the SDK Manager if prompted.
2. Run `./gradlew :app:assembleDebug` and resolve any remaining source-level
   deprecations AGP 8 surfaces (e.g. the `kotlinOptions` DSL is deprecated in
   favor of the `kotlin { compilerOptions { } }` block — still functional in
   Kotlin 1.9.24, can be migrated later).
3. The three jitpack-hosted `Edsuns:*` libraries are old; if they fail to
   resolve or pull in legacy support-library transitive deps, consider
   vendoring/replacing them. `android.enableJetifier=true` is kept in
   `gradle.properties` as a safety net for any transitive legacy deps.
4. Tag `v2.0.0` on `master` once the build is green so JitPack can publish
   `com.github.reaver961025.AdblockAndroid:ad-filter:2.0.0`.
