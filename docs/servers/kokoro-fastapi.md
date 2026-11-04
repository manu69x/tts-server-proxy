# Kokoro-FastAPI — ★★☆ Medium

**Repo:** https://github.com/remsky/Kokoro-FastAPI · **License:** Apache-2.0
**GUI:** optional Web UI · **Cloning:** ❌ preset voices only · **Italian:** ✅ preset voices
**Default port:** `8880`

FastAPI wrapper around **Kokoro-82M**: an OpenAI-compatible speech endpoint
with multi-language preset voices (English US/GB, Spanish, French, Hindi,
**Italian**, Japanese, Brazilian Portuguese, Mandarin), ElevenLabs-compatible
websockets, and an optional integrated Web UI. Very fast even on CPU.

## Install (Windows, native — no Docker needed)

1. Install [uv](https://docs.astral.sh/uv/) (single PowerShell command).
2. Clone the repo and use the direct-run path:

   ```powershell
   git clone https://github.com/remsky/Kokoro-FastAPI
   cd Kokoro-FastAPI
   uv run python -m api.main  # native run; models auto-download
   ```

   (Windows PowerShell helper scripts are also provided upstream; Docker images
   `kokoro-fastapi-cpu` / `kokoro-fastapi-gpu` are the alternative if you prefer
   containers.)

3. Optional Web UI at `http://localhost:8880/web`.

## Connect tts-server-proxy

| App field | Value |
|---|---|
| Endpoint | `http://<pc-ip>:8880` |
| API key | empty (or the key you configured on the server — Bearer is honored) |
| model | `kokoro` |
| voice | Italian presets: `if_sala` (female) or `im_nicola` (male) |
| lang | `it` |

## Gotchas

- **No zero-shot cloning** — Kokoro ships fixed preset voices (the upstream
  "Inno Clone-Tuner" voicepack tooling is explicitly *not* a cloner). If you
  want your own voice, use Chatterbox or AllTalk instead.
- Kokoro is an 82M model: quality is good and latency excellent, but a long
  reference-clip clone (XTTS/Chatterbox style) sounds closer to the original
  speaker.
- First run downloads the model (~1 GB); runs fine on CPU laptops.
