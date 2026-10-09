# Plinky 🎶

A tiny gamified ukulele practice app for Android. Native Kotlin, no accounts, no ads, no network — it just listens to your uke through the microphone.

## What it does

- **Tuner** — autocorrelation pitch detection for G C E A (standard ukulele tuning), with a cents needle and tune up/down hints.
- **Metronome** — 40–208 BPM, 4/4 and 3/4, accented downbeat. Clicks are synthesized directly into a streaming AudioTrack, so timing is sample-accurate.
- **Beat Hero** — a timing game. Beats light up across the bar; strum on the highlighted ones. The mic detects strum onsets and grades each hit Perfect / Good / Miss, with combos, star ratings, and saved personal bests across four difficulty levels.
- **Chord Sprint** — the classic "one-minute changes" drill. Pick a chord pair (C↔Am, F↔G, …), and every detected strum flips the chord diagram. Change count and personal bests are saved per pair.

## How the audio works

- Strum detection runs on a low-passed (~900 Hz) copy of the mic signal, while the metronome clicks are high-pitched (3.0 / 3.8 kHz) sine bursts — so the phone's own clicks don't register as strums.
- Beat Hero has a mic timing-offset slider to calibrate away device audio latency; the setting persists.
- The games detect that you strummed and when, not chord fingering. Spectral chord checking is on the roadmap.

## Building

Requires an Android SDK (platform 35) and JDK 17+.

```
gradle assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`. Minimum Android 8.0 (API 26).

## License

MIT
