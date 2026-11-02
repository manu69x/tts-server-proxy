package it.ttsserverproxy;

import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.Set;

/** Diagnostics: voices negotiated with the framework, config import, service log. */
public class DiagActivity extends androidx.appcompat.app.AppCompatActivity {
    private TextView out;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.attr(this, com.google.android.material.R.attr.colorSurface));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(Ui.header(this, "Diagnostics",
                "Framework voices · config · service log"));

        MaterialCardView toolsC = Ui.card(this);
        LinearLayout tools = Ui.into(this, toolsC);
        tools.addView(Ui.sectionLabel(this, "Tools"));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        MaterialButton imp = Ui.tonal(this, "⬇ Import config");
        imp.setOnClickListener(v -> importConfig());
        row.addView(imp, Ui.lp(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 0, 0, Ui.dp(this, 10), 0));
        MaterialButton log = Ui.tonal(this, "📄 debug.log");
        log.setOnClickListener(v -> readDebugLog());
        row.addView(log, Ui.lp(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 0, 0, 0, 0));
        tools.addView(row);
        root.addView(toolsC);

        MaterialCardView resultC = Ui.card(this);
        LinearLayout result = Ui.into(this, resultC);
        result.addView(Ui.sectionLabel(this, "Result"));
        out = new TextView(this);
        out.setTypeface(Typeface.MONOSPACE);
        out.setTextSize(13);
        out.setTextColor(Ui.attr(this, com.google.android.material.R.attr.colorOnSurface));
        out.setTextIsSelectable(true);
        out.setText("Initializing engine…");
        result.addView(out);
        root.addView(resultC);

        TextView spacer = new TextView(this);
        spacer.setText(" ");
        spacer.setTextSize(20);
        root.addView(spacer);

        scroll.addView(root);
        setContentView(scroll);

        final StringBuilder sb = new StringBuilder();
        final TextToSpeech[] h = new TextToSpeech[1];
        h[0] = new TextToSpeech(this, status -> {
            TextToSpeech tts = h[0];
            sb.append("init: ").append(status).append("\n");
            try {
                sb.append("engine: ").append(tts.getDefaultEngine()).append("\n");
                Set<Voice> voices = tts.getVoices();
                sb.append("total voices: ").append(voices == null ? -1 : voices.size()).append("\n");
                if (voices != null) for (Voice v : voices)
                    sb.append("  • ").append(v.getName()).append(" [").append(v.getLocale()).append("]\n");
                sb.append("current language: ").append(tts.getLanguage()).append("\n");
                sb.append("current voice: ").append(tts.getVoice()).append("\n");
                int r = tts.setLanguage(java.util.Locale.ITALIAN);
                sb.append("setLanguage(it): ").append(r).append("\n");
                int r2 = tts.isLanguageAvailable(new java.util.Locale("it", "IT"));
                sb.append("isLanguageAvailable(it-IT): ").append(r2).append("\n");
            } catch (Throwable t) {
                sb.append("ERR: ").append(t).append("\n");
            }
            out.post(() -> out.append(sb.toString()));
            // also save to a file for adb
            try {
                java.io.FileWriter w = new java.io.FileWriter(
                        new java.io.File(getFilesDir(), "diag.txt"));
                w.write(sb.toString()); w.close();
            } catch (Exception ignored) {}
        }, "it.ttsserverproxy");
    }

    /** Shows the latest lines of the diagnostic log written by LanTtsService. */
    private void readDebugLog() {
        try {
            java.io.File f = new java.io.File(getFilesDir(), "debug.log");
            if (!f.exists()) { out.setText("debug.log missing (no synthesis attempted yet)"); return; }
            java.util.List<String> lines = new java.util.ArrayList<>();
            try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(f))) {
                String line; while ((line = r.readLine()) != null) lines.add(line);
            }
            int from = Math.max(0, lines.size() - 25);
            StringBuilder b = new StringBuilder("debug.log (last " + (lines.size() - from) + "):\n");
            for (int i = from; i < lines.size(); i++) b.append(lines.get(i)).append("\n");
            out.setText(b.toString());
        } catch (Throwable t) {
            out.setText("Error reading log: " + t);
        }
    }

    /**
     * Config restore (debug→release migration / new phone): reads
     * getExternalFilesDir(null)/config-import.json = {"endpoint":...,"api_key":...,"models":[...]}
     * pushed via `adb push`, then deletes the file (one-shot).
     */
    private void importConfig() {
        try {
            java.io.File f = new java.io.File(getExternalFilesDir(null), "config-import.json");
            if (!f.exists()) { out.setText("File missing: " + f.getAbsolutePath()); return; }
            StringBuilder b = new StringBuilder();
            try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(f))) {
                String line; while ((line = r.readLine()) != null) b.append(line);
            }
            org.json.JSONObject c = new org.json.JSONObject(b.toString());
            android.content.SharedPreferences.Editor e =
                    getSharedPreferences("config", MODE_PRIVATE).edit();
            if (c.has("endpoint")) e.putString("endpoint", c.getString("endpoint").trim());
            if (c.has("api_key")) e.putString("api_key", c.getString("api_key").trim());
            int n = 0;
            if (c.has("models")) {
                e.putString("models", c.getJSONArray("models").toString());
                n = c.getJSONArray("models").length();
            }
            e.apply();
            f.delete();
            out.setText("Config imported: endpoint=" + c.optString("endpoint")
                    + ", models=" + n + " (file deleted)");
        } catch (Throwable t) {
            out.setText("Import error: " + t);
        }
    }
}
