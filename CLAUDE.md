# CLAUDE.md

Plinky is a single-module Android app written in Kotlin (package `im.flume.plinky`) for practising the ukulele. It uses the microphone, the views are XML layouts, and it has no network access, accounts or backend.

## Commands

The repo has **no Gradle wrapper** (`gradlew` is absent), so use a system `gradle`. AGP 8.7.3 needs Gradle 8.9 or newer. You also need JDK 17+ and Android SDK platform 35, with `sdk.dir` set in `local.properties` (that file is gitignored) or `ANDROID_HOME` set.

| Purpose | Command | Notes |
|---|---|---|
| Build | `gradle assembleDebug` | The APK is written to `app/build/outputs/apk/debug/`. This is the only command the README documents. |
| Typecheck | `gradle compileDebugKotlin` | The standard AGP task, and the fastest way to check that the code compiles. |
| Lint | `gradle lintDebug` | Stock Android Lint. The repo has no lint config, ktlint or detekt. |
| Test | none | There is no `src/test` or `src/androidTest` and no test dependencies. Don't claim tests passed. |

- None of these commands has been run in this environment, which has no `gradle` or `java`. If you can't run them, say so; don't report a build as green.
- There is no CI config, so nothing checks the code automatically.

## Structure

```
app/src/main/java/im/flume/plinky/
  MainActivity.kt   bottom-nav host; asks for RECORD_AUDIO; starts the mic in onResume and stops mic + metronome in onPause
  Prefs.kt          the only persistence layer (SharedPreferences file "plinky", Int values only)
  Chords.kt         chord shapes + Chord Sprint pairs
  audio/            AppAudio (shared singletons), MicEngine (strum + pitch), Metronome (AudioTrack), Dsp (pitch detection)
  ui/               one *Fragment per tab + custom Canvas Views (*View)
app/src/main/res/
  layout/           activity_main.xml, fragment_<feature>.xml
  menu/bottom_nav.xml, drawable/ic_<feature>.xml
  values/ + values-night/   colors (night only overrides surface/text colours), strings, theme
```

Where new code goes:
- **New tab or screen.** Create `ui/<Name>Fragment.kt` and `res/layout/fragment_<name>.xml`, add an item to `menu/bottom_nav.xml` and a `drawable/ic_<name>.xml` icon, then add a branch to the `when` in `MainActivity.onCreate`.
- **New custom drawing.** Write a `View` subclass in `ui/` that uses an `@JvmOverloads constructor(context, attrs)` and does all its drawing in `onDraw`. See `BeatDotsView` for the pattern.
- **Signal processing.** Put pure math in `audio/Dsp.kt` (no Android imports), and keep threading and I/O in `MicEngine` / `Metronome`.
- **New chord or sprint pair.** Add it to `Chords.kt`. List frets in **G C E A** string order, with 0 meaning an open string.

## Conventions this code follows

- **Shared audio engines only.** Use `AppAudio.mic` and `AppAudio.metro`. Never construct another `MicEngine` or `Metronome`, because only one of each can own the mic or speaker.
- **Listener lifecycle.** Fragments implement `MicEngine.Listener` and/or `Metronome.BeatListener`. They register in `onStart` and unregister (setting `metro.listener = null`) in `onStop`. Real example from `ui/TunerFragment.kt`:
  ```kotlin
  override fun onStart() {
      super.onStart()
      AppAudio.mic.pitchEnabled = true
      AppAudio.mic.addListener(this)
      AppAudio.mic.start(requireContext())
  }
  ```
- **Fragment wiring.** Use the `Fragment(R.layout.fragment_x)` constructor, `private lateinit var` view fields, and `findViewById` in `onViewCreated`. The project doesn't use ViewBinding, Compose, coroutines, ViewModel or DI, so don't introduce them in passing.
- **Threading.** Audio loops run on a named `Thread` (`"plinky-mic"`, `"plinky-metronome"`) at `MAX_PRIORITY`, controlled by a `@Volatile var running` flag and stopped with `join(800)`. Callbacks go to the main thread through `Handler(Looper.getMainLooper()).post`, which means listeners already run on the UI thread. Guard callbacks with `if (!playing || !isAdded) return`.
- **Time.** All timestamps are `System.nanoTime()` nanoseconds. Delays and thresholds are written as nanosecond `Long` literals (`280_000_000L`) or converted explicitly with `/ 1_000_000L`.
- **Strum vs click separation.** `MicEngine` low-passes the signal at about 900 Hz and the metronome clicks are 3000/3800 Hz sine bursts. Keeping those bands apart is what stops the app hearing its own clicks as strums, so lowering the click pitch or raising the low-pass corner will break Beat Hero and Chord Sprint.
- **Persistence.** Use `Prefs.getInt/setInt/addToInt(ctx, key, ...)`. These keys already exist: `"cal_offset"`, `"plinks"`, `"bh_best_${beats}_$level"`, and `"sprint_${a}_${b}"` (from `SprintPair.key`). Renaming a key or a chord `name` silently wipes users' personal bests.
- **Resources.** Static labels belong in `strings.xml`, while score and feedback text is built with inline string templates (`"Best $best%"`). Take colours from `R.color.*` via `ContextCompat.getColor`. If you add a surface or text colour, give it a `values-night` override too.
- **Dialogs.** Use `MaterialAlertDialogBuilder`. The theme is `Theme.Material3.DayNight.NoActionBar`.
- **App config.** The app is portrait-only, and `MainActivity` declares `configChanges`, so rotation doesn't recreate fragments. Don't depend on `savedInstanceState` restoring game state.
- **Dependencies** are only `core-ktx`, `appcompat` and `material`, and versions are hard-coded in `app/build.gradle` because there is no version catalog.

## Boundaries

### Always
- Run `gradle compileDebugKotlin` (or `assembleDebug`) after you change Kotlin or resources. If you can't run it, say so explicitly.
- Unregister every listener you register and stop the metronome in `onStop`, mirroring the existing fragments.
- Check `AppAudio.mic.start(ctx)`'s return value before starting a game. It returns `false` when mic permission is missing, and the code then shows `R.string.mic_needed`.
- Keep the Prefs keys and chord names that already exist unchanged.

### Ask first
- Adding any dependency, Gradle plugin, the Gradle wrapper, or a test framework.
- Adding any manifest permission, especially `INTERNET`. The README promises "no accounts, no ads, no network".
- Changing the DSP constants: the strum threshold, the 160 ms debounce, the low-pass coefficient, the click frequencies, the pitch range of 70–1000 Hz, or the Beat Hero grading windows (110 ms Perfect, 280 ms hit).
- Changing `minSdk 26`, `compileSdk`/`targetSdk 35`, `applicationId`, or `versionCode`/`versionName`.
- Migrating architecture (to Compose, ViewModel, coroutines, or ViewBinding).

### Never
- Commit secrets, including keystores (`*.jks`, `*.keystore`), signing passwords, API keys, or `local.properties`.
- Record, store or transmit microphone audio. Audio is analysed in memory only.
- Run blocking audio I/O on the main thread, or touch Views from the `plinky-*` threads.
- Edit build output (`build/`, `app/build/`), `.gradle/`, or `.idea/`.

## Open questions

- **Release builds.** There is no signing config or release process, and `minifyEnabled false`. How is the app distributed?
- **Gradle version.** With no wrapper the version isn't pinned. Should a wrapper be added?
- **Tests.** There are none. `Dsp.detectPitch` is pure Kotlin and would be the natural first unit test. Is adding JUnit wanted?
- **License.** The README says MIT, but the repo has no `LICENSE` file.

## Mistake ledger

<!-- Append a dated one-line correction here whenever an agent gets something wrong in this repo, so the same mistake is not repeated. -->
