# Chatterbox-TTS-Server (Windows portable) — ★☆☆ Easy

**Repo:** https://github.com/devnen/Chatterbox-TTS-Server · **License:** MIT
**GUI:** ✅ full Web UI · **Cloning:** ✅ reference audio · **Italian:** ✅ Multilingual engine
**Default port:** `8004`

Self-hosts Resemble AI's open-source Chatterbox family (Original,
**Multilingual — 23 languages**, Turbo) behind an OpenAI-compatible API with a
modern Web UI, chunked long-text handling and audiobook generation.

## Why this one first

The **Windows Portable Mode** is the lowest-friction path that exists today:
the launcher embeds its own Python, so the whole folder is self-contained —
copy it anywhere (even a USB stick) and double-click `start.bat`. **No Python
installation required.**

## Install (Windows 10/11, ~10 minutes)

1. Download/clone the repo: `git clone https://github.com/devnen/Chatterbox-TTS-Server`
   (or download the release zip).
2. Run `start.bat` → first-time setup: choose **Portable Mode** (selected by default).
3. Open `http://localhost:8004` → the Web UI.
4. In the UI: pick the **Multilingual** engine (required for Italian — do *not*
   use the Original engine for Italian), select a built-in voice or upload a
   short reference clip for cloning.
5. Models download on first run (a few GB; NVIDIA GPU recommended — CPU works
   but is slower). RTX 30/40/50 supported.

## Connect tts-server-proxy

| App field | Value |
|---|---|
| Endpoint | `http://<pc-ip>:8004` |
| API key | empty (no auth by default — keep it LAN-only) |
| model | anything (server uses the engine selected in its UI), e.g. `chatterbox` |
| voice | a built-in voice name or your reference clip name (see `GET /v1/audio/voices`) |
| lang | `it` |

## Gotchas

- The OpenAI endpoint has **no language field**: set the language in the server
  UI (`SUPPORTED_LANGUAGES` dropdown for the Multilingual engine).
- v2.0+ added `/v1/audio/voices` (OpenAI-compatible voice listing) and an
  optional streaming `/tts` endpoint — the plain `/v1/audio/speech` is what our
  app uses.
- Interactive API reference lives at `http://<pc-ip>:8004/docs`.
