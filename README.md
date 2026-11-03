# Runner core · 10 min

A randomized 10-minute bodyweight core workout for runners: anti-extension, anti-rotation,
anti-lateral-flexion, hip/glute, and a short finisher. One self-contained `index.html`, no build step.

- Easy / moderate / hard change exercise selection and work-to-rest ratio (1:1, 2:1, 3:1).
- Mix of 60s holds and shorter sets, totaling about 10:00 including rest.
- Hands-free timer with beeps, voice cues, side-switch prompts, and stick-figure diagrams.
- Recent sessions are kept in the browser's local storage so consecutive days don't repeat.

Open `index.html` in any browser, or use the hosted copy on GitHub Pages.

## Android TV app

`tv/` is a small Android TV app that opens the page full-screen, uses the TV's text-to-speech for voice
cues, keeps the screen on, and falls back to a bundled copy of `index.html` when offline. It declares a
[CleanYourTV](https://github.com/Gathin23/cleanyourtv) module card, so it shows as a tile on that launcher.

Build with JDK 17 and Android SDK 34: `cd tv && ./gradlew assembleRelease`.
