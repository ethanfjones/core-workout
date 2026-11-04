# Runner training

Bodyweight sessions for a runner, from one self-contained `index.html` with no build step:

- **Pre-run warm-up** (5 min): dynamic mobility and running drills.
- **Core workout** (5, 10 or 15 min): randomized anti-extension, anti-rotation, anti-lateral-flexion,
  hip/glute and a finisher. Easy / moderate / hard change exercise selection and work-to-rest ratio (2:1, 3:1, 4:1).
- **Post-run stretch** (7 min): static holds, one per muscle group plus both calf stretches.

Hands-free timer with beeps, a recorded coach voice, side-switch prompts and animated diagrams.
Recent sessions are kept in the browser's local storage so consecutive days don't repeat.

Hosted at https://core-workout.gonzoscreen.com (GitHub Pages).

## Coach voice

`audio/` holds one MP3 per spoken line, recorded with the open-source [Kokoro](https://github.com/thewh1teagle/kokoro-onnx)
TTS model. After changing exercise names or cues, re-record only what changed:

```bash
node tools/voice/dump_lines.js > /tmp/lines.json
venv/bin/python tools/voice/make_voice.py /tmp/lines.json --model-dir <folder with the Kokoro model files>
```

Setup steps and the voice settings are at the top of `tools/voice/make_voice.py`. If a clip is missing,
the app falls back to the device's text-to-speech for that line.

## Android TV app

`tv/` is a small Android TV app that opens the page full-screen, uses the TV's text-to-speech for voice
cues, keeps the screen on, and falls back to a bundled copy of `index.html` when offline. It declares a
[CleanYourTV](https://github.com/Gathin23/cleanyourtv) module card, so it shows as a tile on that launcher.

Build with JDK 17 and Android SDK 34: `cd tv && ./gradlew assembleRelease`.
