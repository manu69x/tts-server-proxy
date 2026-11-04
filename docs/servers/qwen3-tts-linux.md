# Qwen3-TTS on a Linux GPU box — ★★★ Advanced

**License:** model terms apply (check Alibaba's Qwen3-TTS license) · **Cloning:** ✅ zero-shot · **Italian:** ✅
**No GUI** (CLI + config files) · **This is what the author runs in production**

The highest-quality path: Qwen3-TTS served behind an OpenAI-compatible
`/v1/audio/speech` endpoint on a Linux GPU machine (NVIDIA or AMD/Vulkan),
optionally fronted by an orchestrator (llama-swap) and a wake-on-LAN proxy so
the box can sleep between uses.

## Architecture (what scales well)

```
Android (tts-server-proxy)
   → WoL proxy (FastAPI: magic packet + health poll + forward)
      → llama-swap (model orchestration, health-gated routing)
         → Qwen3-TTS engine (OpenAI-compatible /v1/audio/speech)
```

You don't need all the layers — a single FastAPI/uvicorn process exposing
`/v1/audio/speech` in front of a Qwen3-TTS engine is enough. Useful building
blocks (all open source):

- **llama-swap** — model orchestration: per-model engine processes, health
  checks, unload endpoints (great with several models/voices).
- **faster-qwen3-tts** — community fast Qwen3-TTS implementation with CUDA
  graphs; a 1.7B variant answers in ~1-3 s per sentence on a V100-class GPU.
- **Phoenix-style WoL proxy** — wakes a sleeping box (S5) on demand; clients
  need a long read timeout for the cold path (wake + boot + model load), the
  app tolerates up to **420 s** by design.

## Connect tts-server-proxy

| App field | Value |
|---|---|
| Endpoint | `http://<server-ip>:<port>` (or a Tailscale IP — the author's setup runs over Tailscale) |
| API key | whatever your proxy/engine requires |
| model / voice | your engine's model id and voice/clone name — one app entry per voice |
| lang | `it` / `en` |

## Gotchas

- **Cold starts are real**: if the GPU box sleeps, the first request includes
  wake-on-LAN + boot + model load. The app waits up to 7 minutes and retries
  fast failures, but keep the box awake if you do interactive use.
- Engines are picky about decoder precision on some GPUs (fp16 artifacts on
  Vulkan/AMD — int8 decoder fixed it for the author).
- Same text + same seed does **not** guarantee identical audio (nondeterministic
  kernels); cache approved takes text→WAV if you need repeatable results.
- Linux-only for practical purposes; on Windows use WSL2 or pick one of the
  native Windows servers in this folder.
