"""Record the coach voice: one MP3 per spoken line into audio/, using the Kokoro TTS model.

Only lines whose text or voice changed since the last run are re-recorded (tracked in audio/lines.json).

Setup (once):
    python3 -m venv venv && venv/bin/pip install kokoro-onnx soundfile lameenc
    curl -LO https://github.com/thewh1teagle/kokoro-onnx/releases/download/model-files-v1.0/kokoro-v1.0.onnx
    curl -LO https://github.com/thewh1teagle/kokoro-onnx/releases/download/model-files-v1.0/voices-v1.0.bin

Run from the repo root:
    node tools/voice/dump_lines.js > /tmp/lines.json
    venv/bin/python tools/voice/make_voice.py /tmp/lines.json --model-dir <folder with the two model files>
"""
import argparse
import json
import os

import lameenc
import numpy as np
from kokoro_onnx import Kokoro

# An older British male voice: a blend of two Kokoro voices, delivered a little slower than normal.
VOICE = {"bm_george": 0.6, "bm_lewis": 0.4}
SPEED = 0.9
LANG = "en-gb"
SAMPLE_RATE_OUT = 24000
BITRATE_KBPS = 48

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "audio")


def encode_mp3(samples, sr):
    pcm = (np.clip(samples, -1, 1) * 32767).astype(np.int16)
    enc = lameenc.Encoder()
    enc.set_bit_rate(BITRATE_KBPS)
    enc.set_in_sample_rate(sr)
    enc.set_channels(1)
    enc.set_quality(2)
    return enc.encode(pcm.tobytes()) + enc.flush()


def trim(samples, sr, pad=0.05, threshold=0.01):
    """Cut leading/trailing silence so clips chain without long gaps."""
    loud = np.where(np.abs(samples) > threshold)[0]
    if not len(loud):
        return samples
    a = max(0, loud[0] - int(sr * pad))
    b = min(len(samples), loud[-1] + int(sr * pad))
    return samples[a:b]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("lines")
    ap.add_argument("--model-dir", default=".")
    args = ap.parse_args()

    lines = json.load(open(args.lines))
    os.makedirs(OUT, exist_ok=True)
    state_path = os.path.join(OUT, "lines.json")
    state = json.load(open(state_path)) if os.path.exists(state_path) else {}
    voice_id = json.dumps([VOICE, SPEED, LANG])

    kokoro = Kokoro(os.path.join(args.model_dir, "kokoro-v1.0.onnx"), os.path.join(args.model_dir, "voices-v1.0.bin"))
    style = sum(kokoro.get_voice_style(name) * w for name, w in VOICE.items())

    todo = [k for k, t in lines.items()
            if state.get(k) != {"text": t, "voice": voice_id} or not os.path.exists(os.path.join(OUT, k + ".mp3"))]
    print(f"{len(lines)} lines, {len(todo)} to record")
    for i, key in enumerate(todo, 1):
        samples, sr = kokoro.create(lines[key], voice=style, speed=SPEED, lang=LANG)
        with open(os.path.join(OUT, key + ".mp3"), "wb") as f:
            f.write(encode_mp3(trim(samples, sr), sr))
        state[key] = {"text": lines[key], "voice": voice_id}
        if i % 25 == 0 or i == len(todo):
            print(f"  {i}/{len(todo)}")
            json.dump(state, open(state_path, "w"), indent=1, sort_keys=True)

    # drop clips for lines the app no longer says
    for key in list(state):
        if key not in lines:
            path = os.path.join(OUT, key + ".mp3")
            if os.path.exists(path):
                os.remove(path)
            del state[key]
    json.dump(state, open(state_path, "w"), indent=1, sort_keys=True)


if __name__ == "__main__":
    main()
