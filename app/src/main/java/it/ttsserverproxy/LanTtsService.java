package it.ttsserverproxy;

import android.media.AudioFormat;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.SynthesisRequest;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

public class LanTtsService extends TextToSpeechService {
    private static final String TAG = "TtsServerProxy";
    private static final String CH_SYNTH = "synthesis";
    private static final int NOTIF_ID = 1;
    private volatile boolean stopped;

    // ---- synthesis notification + foreground service: without startForeground,
    // Android freezes/kills the process during long syntheses (cold start ~3 min) ----
    private void notifyStart(String text) {
        try {
            android.app.NotificationManager nm =
                    (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm == null) return;
            android.app.NotificationChannel ch =
                    new android.app.NotificationChannel(CH_SYNTH, "Speech synthesis",
                            android.app.NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
            String body = text.length() > 60 ? text.substring(0, 60) + "…" : text;
            android.app.Notification n = new android.app.Notification.Builder(this, CH_SYNTH)
                    .setSmallIcon(R.drawable.ic_stat_synth)
                    .setColor(getColor(R.color.notification_accent))
                    .setContentTitle("Speech synthesis")
                    .setContentText(body)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setCategory(android.app.Notification.CATEGORY_PROGRESS)
                    .setProgress(0, 0, true)
                    .build();
            try {
                startForeground(NOTIF_ID, n);   // process protected until synthesis ends
            } catch (Throwable t2) {
                // if the system rejects the foreground promotion (rare, engine bound by the framework)
                if (checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                        == android.content.pm.PackageManager.PERMISSION_GRANTED)
                    nm.notify(NOTIF_ID, n);
            }
        } catch (Throwable ignored) {}
    }

    private void notifyDone() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE);
            android.app.NotificationManager nm =
                    (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(NOTIF_ID);
        } catch (Throwable ignored) {}
    }

    private void dbg(String msg) {
        try {
            java.io.File f = new java.io.File(getFilesDir(), "debug.log");
            java.io.FileWriter w = new java.io.FileWriter(f, true);
            w.write(System.currentTimeMillis() + " " + msg + "\n");
            w.close();
        } catch (Exception ignored) {}
    }

    // ---- declared languages: union of the configured models' languages ----
    private List<JSONObject> models() {
        List<JSONObject> ms = MainActivity.loadModels(this);
        // in-code fallback: the engine keeps talking even with empty prefs
        // (fresh install / reboot without ever opening the app)
        return ms.isEmpty() ? MainActivity.defaultModels() : ms;
    }

    @Override public int onIsLanguageAvailable(String lang, String country, String variant) {
        if (lang == null || lang.isEmpty()) return TextToSpeech.LANG_NOT_SUPPORTED;
        // normalize ISO-3 → ISO-2 (the framework may pass "ita")
        String l = iso2(lang);
        for (JSONObject m : models()) {
            String ml = MainActivity.androidLang(m.optString("lang", "it"));
            if (ml.equalsIgnoreCase(l)) {
                return (country == null || country.isEmpty()) ? TextToSpeech.LANG_AVAILABLE
                        : TextToSpeech.LANG_COUNTRY_AVAILABLE;
            }
        }
        return TextToSpeech.LANG_NOT_SUPPORTED;
    }

    private static String iso2(String l) {
        switch (l.toLowerCase()) {
            case "ita": return "it"; case "eng": return "en"; case "deu": case "ger": return "de";
            case "fra": case "fre": return "fr"; case "spa": return "es"; case "por": return "pt";
            case "nld": case "dut": return "nl"; case "rus": return "ru"; case "jpn": return "ja";
            case "kor": return "ko"; case "ara": return "ar"; case "hin": return "hi";
            case "pol": return "pl"; case "tur": return "tr"; case "swe": return "sv";
            case "dan": return "da"; case "fin": return "fi"; case "nor": return "no";
            case "ces": case "cze": return "cs"; case "ell": case "gre": return "el";
            case "heb": return "he"; case "hun": return "hu"; case "ind": return "id";
            case "ron": case "rum": return "ro"; case "tha": return "th"; case "ukr": return "uk";
            case "vie": return "vi"; case "zho": case "chi": return "zh";
            default: return l;
        }
    }

    @Override public String[] onGetLanguage() {
        JSONObject first = models().isEmpty() ? null : models().get(0);
        String l = first == null ? "it" : MainActivity.androidLang(first.optString("lang", "it"));
        return new String[]{l, "", ""};
    }

    @Override protected int onLoadLanguage(String lang, String country, String variant) {
        return onIsLanguageAvailable(lang, country, variant);
    }

    @Override protected void onStop() { stopped = true; }

    // ---- Android voices = configured models ----
    // Note: TextToSpeechService exposes voices via onGetDefaultVoiceNameFor;
    // the framework builds the voice list from onIsLanguageAvailable per language.
    @Override public String onGetDefaultVoiceNameFor(String lang, String country, String variant) {
        try {
            String l = iso2(lang == null ? "" : lang);
            for (JSONObject m : models()) {
                String ml = MainActivity.androidLang(m.optString("lang", "it"));
                if (ml.equalsIgnoreCase(l)) return m.optString("label", m.optString("model"));
            }
            return models().isEmpty() ? null : models().get(0).optString("label", "default");
        } catch (Throwable t) {
            dbg("onGetDefaultVoiceNameFor ERR: " + t);
            return null;
        }
    }

    @Override public java.util.List<android.speech.tts.Voice> onGetVoices() {
        java.util.List<android.speech.tts.Voice> out = new ArrayList<>();
        try {
            for (JSONObject m : models()) {
                String ml = m.optString("lang", "it");
                String al = MainActivity.androidLang(ml);
                out.add(new android.speech.tts.Voice(
                        m.optString("label", m.optString("model")),
                        new java.util.Locale(al),
                        android.speech.tts.Voice.QUALITY_HIGH,
                        android.speech.tts.Voice.LATENCY_NORMAL,
                        false, new java.util.HashSet<String>()));
            }
        } catch (Throwable t) {
            dbg("onGetVoices ERR: " + t);
        }
        return out;
    }

    @Override public int onIsValidVoiceName(String voiceName) {
        for (JSONObject m : models())
            if (voiceName.equals(m.optString("label", "")) || voiceName.equals(m.optString("model", "")))
                return TextToSpeech.SUCCESS;
        return TextToSpeech.ERROR;
    }

    @Override public int onLoadVoice(String voiceName) {
        return onIsValidVoiceName(voiceName);
    }

    // the client picks the model via voice name: getVoiceName / TTS voice selection
    private JSONObject modelFor(String requestedVoiceName, String lang) {
        List<JSONObject> ms = models();
        // 1) match by voice name
        if (requestedVoiceName != null) {
            for (JSONObject m : ms) {
                if (requestedVoiceName.equals(m.optString("label", "")) ||
                    requestedVoiceName.equals(m.optString("model", ""))) return m;
            }
        }
        // 2) fallback: first model with that language
        for (JSONObject m : ms) {
            if (lang != null && (MainActivity.androidLang(m.optString("lang", "it")).equalsIgnoreCase(lang)
                    || m.optString("lang", "it").equalsIgnoreCase(lang))) return m;
        }
        // 3) last resort: first model in the list
        return ms.isEmpty() ? null : ms.get(0);
    }

    @Override protected void onSynthesizeText(SynthesisRequest request, SynthesisCallback callback) {
        stopped = false;
        try {
            String text = request.getText() == null ? "" : request.getText().toString();
            if (text.trim().isEmpty()) { callback.error(TextToSpeech.ERROR); return; }

            JSONObject model = modelFor(request.getVoiceName(), request.getLanguage());
            if (model == null) { dbg("no model configured"); callback.error(TextToSpeech.ERROR); return; }

            String base = prefs().getString("endpoint", MainActivity.DEFAULT_ENDPOINT);
            if (base.isEmpty()) base = MainActivity.DEFAULT_ENDPOINT; // endpoint cleared by mistake
            String apiKey = prefs().getString("api_key", "");

            long t0 = System.currentTimeMillis();
            Log.i(TAG, "synth: model=" + model.optString("model") + " voice=" + model.optString("voice")
                    + " lang=" + model.optString("lang") + " chars=" + text.length());
            notifyStart(text);
            dbg("POST " + base + " model=" + model.optString("model") + " (requested voice=" + request.getVoiceName() + ")");
            byte[] audio = post(base, apiKey, model.optString("model"), model.optString("voice"), text);
            Log.i(TAG, "http ok: " + audio.length + " bytes in " + (System.currentTimeMillis() - t0) + " ms");
            dbg("http ok: " + audio.length + " bytes in " + (System.currentTimeMillis() - t0) + " ms");

            AudioDecoder.Pcm pcm;
            if (isWav(audio)) {
                pcm = parseWav(audio);   // native WAV from the server: zero decoding
                dbg("parseWav ok: rate=" + pcm.rate + " ch=" + pcm.channels + " bytes=" + pcm.data.length);
            } else {
                pcm = AudioDecoder.decode(audio);  // compatibility fallback (MP3 etc.)
                dbg("decode MP3 ok: rate=" + pcm.rate + " ch=" + pcm.channels + " bytes=" + pcm.data.length);
            }
            if (stopped) { notifyDone(); return; } // request already cancelled: stay silent, no error dialogs
            int sr = callback.start(pcm.rate, AudioFormat.ENCODING_PCM_16BIT, pcm.channels);
            dbg("callback.start=" + sr);
            if (sr != TextToSpeech.SUCCESS) return;
            int chunk = Math.max(1024, callback.getMaxBufferSize());
            for (int pos = 0; pos < pcm.data.length && !stopped; pos += chunk) {
                int len = Math.min(chunk, pcm.data.length - pos);
                if (callback.audioAvailable(pcm.data, pos, len) != TextToSpeech.SUCCESS) return;
            }
            if (stopped) { dbg("stopped late"); notifyDone(); return; } // silent: the client is gone
            else { dbg("done in " + (System.currentTimeMillis() - t0) + " ms"); notifyDone(); callback.done(); }
        } catch (Exception e) {
            dbg("EXCEPTION: " + e);
            Log.e(TAG, "synth failed: " + e);
            notifyDone();
            callback.error(TextToSpeech.ERROR);
        }
    }

    private android.content.SharedPreferences prefs() {
        return getSharedPreferences("config", MODE_PRIVATE);
    }

    /** POST with retries on FAST failures (Tailscale blip after phone idle):
     *  if an attempt dies within 15 s we retry, otherwise it is a real failure
     *  (read timeout on a cold server) and we don't insist. */
    private byte[] post(String base, String apiKey, String model, String voice, String text) throws Exception {
        IOException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            long t0 = System.currentTimeMillis();
            try {
                return postOnce(base, apiKey, model, voice, text);
            } catch (IOException e) {
                last = e;
                if (stopped || System.currentTimeMillis() - t0 > 15000) throw e;
                dbg("attempt " + (attempt + 1) + " failed (" + e + "), retrying in 1.5 s");
                Thread.sleep(1500);
            }
        }
        throw last;
    }

    private byte[] postOnce(String base, String apiKey, String model, String voice, String text) throws Exception {
        URL url = new URL(base.replaceAll("/+$", "") + "/v1/audio/speech");
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        c.setRequestMethod("POST"); c.setConnectTimeout(7000);
        // long read: the server may be asleep (WoL wake + boot + model load ~3 min)
        c.setReadTimeout(420000);
        c.setDoOutput(true); c.setRequestProperty("Content-Type", "application/json");
        if (apiKey != null && !apiKey.isEmpty()) c.setRequestProperty("Authorization", "Bearer " + apiKey);
        // response_format wav: the server returns native PCM WAV (no MP3)
        String json = "{\"model\":\"" + esc(model) + "\",\"voice\":\"" + esc(voice)
                + "\",\"input\":\"" + esc(text) + "\",\"response_format\":\"wav\"}";
        try (OutputStream out = c.getOutputStream()) { out.write(json.getBytes(StandardCharsets.UTF_8)); }
        int code = c.getResponseCode();
        InputStream in = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        byte[] buf = new byte[8192]; int n;
        while (in != null && (n = in.read(buf)) != -1) data.write(buf, 0, n);
        byte[] result = data.toByteArray();
        if (code < 200 || code >= 300) throw new IOException("HTTP " + code + ": "
                + new String(result, StandardCharsets.UTF_8));
        return result;
    }

    private String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
    }

    private static boolean isWav(byte[] w) {
        return w.length > 12 && w[0]=='R' && w[1]=='I' && w[2]=='F' && w[3]=='F' && w[8]=='W' && w[9]=='A' && w[10]=='V' && w[11]=='E';
    }

    private AudioDecoder.Pcm parseWav(byte[] w) throws IOException {
        if (!isWav(w)) throw new IOException("Not a valid RIFF WAV response");
        int fmt = -1, channels = -1, rate = -1, bits = -1, dataStart = -1, dataLen = 0;
        int pos = 12;
        while (pos + 8 <= w.length) {
            String id = new String(w, pos, 4, StandardCharsets.US_ASCII);
            int size = leInt(w, pos + 4); int start = pos + 8;
            if (size < 0 || (long) start + size > w.length) throw new IOException("Truncated WAV chunk");
            if ("fmt ".equals(id) && size >= 16) { fmt=leShort(w,start); channels=leShort(w,start+2); rate=leInt(w,start+4); bits=leShort(w,start+14); }
            if ("data".equals(id)) { dataStart=start; dataLen=size; }
            pos = start + size + (size % 2);
        }
        if (fmt != 1 || bits != 16 || dataStart < 0) throw new IOException("WAV is not PCM16: fmt=" + fmt + " bits=" + bits);
        byte[] pcm = new byte[dataLen];
        System.arraycopy(w, dataStart, pcm, 0, Math.min(dataLen, w.length - dataStart));
        return new AudioDecoder.Pcm(rate, channels, pcm);
    }

    private static int leInt(byte[] b, int o) {
        return (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8) | ((b[o+2] & 0xFF) << 16) | ((b[o+3] & 0xFF) << 24);
    }
    private static int leShort(byte[] b, int o) { return (b[o] & 0xFF) | ((b[o+1] & 0xFF) << 8); }
}
