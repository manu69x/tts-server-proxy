# Compatible TTS servers

`tts-server-proxy` speaks the **OpenAI `/v1/audio/speech` dialect**: it POSTs
`{model, input, voice, response_format}` with Bearer auth and plays back the
returned WAV. Any server that implements that contract works — here are the
ones we verified, with honest difficulty ratings.

| Server | Difficulty | GUI | Voice cloning | Italian | Native Windows | License |
|---|---|---|---|---|---|---|
| [Chatterbox-TTS-Server](chatterbox-tts-server.md) | ★☆☆ Easy | Web UI | ✅ reference audio | ✅ (Multilingual engine) | ✅ **Portable, zero install** | MIT |
| [Kokoro-FastAPI](kokoro-fastapi.md) | ★★☆ Medium | Optional Web UI | ❌ (preset voices only) | ✅ preset voices | ✅ PowerShell/uv (or Docker) | Apache-2.0 |
| [AllTalk TTS V2](alltalk-tts-v2.md) | ★★☆ Medium | Web panel | ✅ XTTS/F5 | ✅ (XTTS or Piper it) | ✅ setup script | AGPL-3.0 |
| [Qwen3-TTS on Linux](qwen3-tts-linux.md) | ★★★ Advanced | ❌ CLI + config | ✅ zero-shot | ✅ | ❌ (Linux/WSL2) | varies |

**Which one for me?**

- *"I just want it to work on my Windows PC, no installs"* → **Chatterbox portable mode** (`start.bat`, Python embedded — runs from a USB stick).
- *"I want preset voices, maximum simplicity, maybe CPU-only"* → **Kokoro-FastAPI**.
- *"I want XTTS voice cloning with a battle-tested Windows setup"* → **AllTalk V2**.
- *"I already run a Linux GPU box / want the best Italian quality"* → **Qwen3-TTS** (what the author runs).

## Connecting the app (same for every server)

In the app (**DiagActivity → ⬇ Import config** or directly in the UI):

- **Endpoint**: `http://<server-ip>:<port>` — no trailing `/v1`, the app appends `/v1/audio/speech` itself.
- **API key**: whatever the server expects; leave empty if the server has none.
- **Models**: one entry per Android voice. `label` is the name Android shows,
  `model`/`voice` are the server-side identifiers, `lang` is `it`/`en`/…

Example:

```json
{
  "endpoint": "http://192.168.1.50:8004",
  "api_key": "",
  "models": [
    { "label": "Italian voice", "model": "chatterbox", "voice": "my-clone", "lang": "it" }
  ]
}
```

> **Security note:** these servers are meant for your LAN. Don't port-forward
> them to the internet; if you need remote access use a VPN (Tailscale works
> great — the author's setup runs over it).
