# AllTalk TTS V2 — ★★☆ Medium

**Repo:** https://github.com/erew123/alltalk_tts (V2 lives on the `alltalkbeta` branch) · **License:** AGPL-3.0
**GUI:** ✅ Web admin panel · **Cloning:** ✅ XTTS / F5-TTS · **Italian:** ✅ (XTTS, or a Piper Italian model)
**Default port:** `7851`

A long-running Windows-first TTS server (standalone *or* as a
text-generation-webui extension) that bundles several engines — Coqui XTTS,
Piper, F5-TTS, Coqui VITS, Parler-TTS — behind one server with a Web admin
panel, finetuning tools and a bulk generator.

## Install (Windows 10/11)

Prerequisites: **Git**, **Microsoft C++ Build Tools** (needed to compile some
Python packages), Windows SDK, and **eSpeak NG**.

1. Clone the V2 branch:

   ```bat
   git clone -b alltalkbeta https://github.com/erew123/alltalk_tts
   cd alltalk_tts
   atsetup.bat
   ```

2. Choose **Standalone Installation** in the setup utility and follow the prompts.
3. Start the server and open the admin panel (default `http://localhost:7851`).
4. In the panel: select the XTTS engine (best cloning + Italian), configure the
   voices — each OpenAI voice name (`alloy`, `echo`, `fable`, `nova`, `onyx`,
   `shimmer`) maps to a real voice/clone you choose.

## Connect tts-server-proxy

| App field | Value |
|---|---|
| Endpoint | `http://<pc-ip>:7851` |
| API key | empty, or the key set in AllTalk's OpenAI settings |
| model | `tts-1` (**ignored by AllTalk** — it uses the engine selected in its panel) |
| voice | `alloy` … `shimmer` — each mapped server-side to your voice/clone |
| lang | `it` |

## Gotchas

- **`model` is required but ignored**: engine switching happens in the AllTalk
  UI, not via the API model field. One OpenAI voice = one mapped clone; if you
  want several Android voices, map several voice names in the panel.
- **No language field** on the endpoint — configure the engine/language
  server-side (XTTS is multilingual; Piper needs an Italian voice model).
- Setup is heavier than Chatterbox's portable mode (build tools + eSpeak NG),
  but the project is very mature and well documented.
