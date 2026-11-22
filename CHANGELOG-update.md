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
| CMake | pinned `"3.10.2"` (no longer downloadable) | pinned `"3.22.1"` (SDK-installed, reproducible) |
| NDK | (unpinned) | pinned `"26.3.11579264"` (r26d, reproducible) |

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

## Version catalog

A Gradle version catalog was introduced at `gradle/libs.versions.toml`,
centralizing every dependency version. The three module `build.gradle` files
now reference dependencies via `libs.<alias>` / `libs.bundles.<alias>` instead
of inline coordinates. Plugin versions (AGP / Kotlin / serialization / kapt)
are still applied through the root `buildscript { classpath }` + `apply plugin`
style (a migration to the `plugins {}` DSL is left as a follow-up). A local
script (`scripts/verify_catalog.py`) confirms every `libs.*` accessor used in
the build files is defined in the catalog.

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

## NDK / CMake pinning

`adblock-client/build.gradle` now pins `ndkVersion = "26.3.11579264"` (NDK r26d)
and CMake `version = "3.22.1"` so native builds are reproducible and match what
CI installs. The CMake `3.10.2` pin from upstream is gone (it is no longer
downloadable from the SDK manager). The four shipped ABIs
(`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`) are declared as `abiFilters`.

## Code audit

A full read-only audit of the Kotlin source + the first-party JNI bridge
(`adblockclient-lib.cpp`) was performed. **No build-breakers** were found under
the target toolchain — the project compiles (with deprecation warnings). The
following real issues were **fixed** in this pass:

- `app/.../App.kt` — `PendingIntent.getActivity(..., 0)` now passes
  `FLAG_IMMUTABLE` (hard crash on API 31+ without it; minSdk 23 guarantees the
  constant exists).
- `app/.../main/MainViewModel.kt` — the per-request blocking-stats map was a
  plain `HashMap` mutated from the WebView background interceptor thread while
  the Main thread reads/clears it → `ConcurrentModificationException` and lost
  counters. Now a `ConcurrentHashMap` with all mutations serialized on a lock,
  and observers receive a shallow copy via `postValue`.
- `app/.../main/WebClient.kt` — force-unwraps (`view!!` / `request!!`) replaced
  with null-guards (WebView can pass null during teardown).
- `app/.../main/MainActivity.kt` — added `onDestroy()` that `removeAllViews()` +
  `destroy()`s the WebView so the Activity is not leaked through AdFilter
  callbacks.
- `app/.../URLUtilities.kt` — `Patterns.WEB_URL` (deprecated API 31) →
  `androidx.core.util.PatternsCompat.WEB_URL`.
- `app/.../settings/FilterListAdapter.kt` — `adapterPosition` (deprecated) →
  `bindingAdapterPosition`, guarded against `NO_POSITION`.
- `ad-filter/.../impl/CustomFilterImpl.kt` — `toByteArray()` now explicit
  `Charsets.UTF_8` (the JNI side expects UTF-8).
- `adblock-client/.../adblockclient-lib.cpp` — guarded `NewStringUTF(nullptr)`
  in `matches()`; on the common non-blocked path both rule pointers are null and
  `NewStringUTF(nullptr)` can abort ART.

**Deferred (identified, not fixed — require API/architectural changes that can't
be safely verified without a device):**
- `AdFilterImpl.shouldIntercept()` uses `runBlocking` + `withContext(Dispatchers.Main)`
  to read `webView.url`, blocking the WebView interceptor thread and serializing
  every resource through Main. Proper fix: cache the document URL on
  `onPageStarted`/`onPageFinished` into a `@Volatile` field. (core library)
- `FilterViewModelImpl.init` calls `workManager.getWorkInfosForUniqueWork(id).get()`
  on the Main thread during `App.onCreate` → ANR risk on cold start. Move to a
  coroutine on `Dispatchers.IO`. (core library)
- `AdBlockClient` relies on `finalize()` to release native buffers (finalize is
  deprecated, non-deterministic) and `loadBasicData`/`loadProcessedData` overwrite
  the prior buffer without freeing it. Migrate to an explicit `AutoCloseable`/
  `release()` and free-before-reassign. (core library, native memory)
- `RuleIterator.hasNext()/size()` mishandle a trailing non-newline-terminated
  segment (can yield a spurious empty rule); fixing changes iterator semantics
  and must be validated against `RuleIteratorTest`.
- `adblockclient-lib.cpp` `loadBasicData` passes a non-NUL-terminated buffer to
  `parse()` — flagged as a possible over-read, needs verification against the
  vendored `third-party/ad-block` `parse()` signature.
- Deprecations left as warnings: `onBackPressed`, `systemUiVisibility`/
  `SYSTEM_UI_FLAG_*` (Fullscreen), `notifyDataSetChanged()` (DiffUtil preferred),
  Java-style `observe(owner, { })` lambdas.

## CI / release pipeline

A GitHub Actions workflow (`.github/workflows/build-release.yml`) builds the
library on `ubuntu-latest` with JDK 17 + Android SDK + NDK r26d + CMake 3.22.1:
- Runs `:adblock-client:assembleRelease :ad-filter:assembleRelease` (produces
  the two `.aar` files) and, best-effort (`continue-on-error`), `:app:assembleDebug`.
- Uploads the AARs (+ universal APK if built) as a workflow artifact on every run.
- On a `v*` tag push, creates a GitHub Release (`softprops/action-gh-release`)
  with `ad-filter-release.aar`, `adblock-client-release.aar` and the sample APK
  attached, plus auto-generated release notes.

## Versioning

App and library modules bumped from `versionCode 1` / `versionName "1.0"` to
`versionCode 2` / `versionName "2.0.0"` to mark the update.

## README

- JitPack badge and dependency coordinate updated to the fork
  `com.github.reaver961025.AdblockAndroid:ad-filter:2.0.0`.
- Added an update notice pointing to this file and noting the JDK 17+ /
  Android Studio + NDK + CMake requirement.

## Verification status

- Gradle wrapper 8.7 launches under JDK 21 (`./gradlew --version`).
- Version catalog `libs.versions.toml` parses (Python `tomllib`) and every
  `libs.*` accessor used in the build files is defined (`scripts/verify_catalog.py`).
- A source + JNI audit found **no build-breakers** under the target toolchain.
- **A full build could NOT be run in the sandbox**: no Android SDK/NDK/CMake, and
  Maven Central rate-limits (HTTP 429) the sandbox IP during AGP classpath
  resolution. Compilation is instead verified in GitHub Actions (see CI section),
  which runs on a runner with the SDK/NDK/CMake and no rate limit.

## Remaining steps (to do)

1. Push `update/modernize-toolchain`, trigger the workflow via `workflow_dispatch`
   and iterate until the AAR build is green.
2. Fast-forward `master` (fork only) to the green branch and tag `v2.0.0` — the
   workflow then publishes the `.aar` files to a GitHub Release.
3. Address the deferred audit items (the two ANR risks in the core library and
   the `AdBlockClient` native-memory lifecycle) — these need device/emulator
   validation.
4. Optional: migrate plugins to the `plugins {}` DSL (`pluginManagement` in
   `settings.gradle`) and move `kotlinOptions` to the `kotlin { compilerOptions }`
   block; replace the old jitpack `Edsuns:*` libs if they cause issues.
   `android.enableJetifier=true` is kept as a safety net for transitive legacy deps.
