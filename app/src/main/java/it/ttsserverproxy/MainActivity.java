package it.ttsserverproxy;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends androidx.appcompat.app.AppCompatActivity {
    // Languages (BCP-47 → label)
    static final Map<String, String> LANGUAGES = new LinkedHashMap<>();
    static {
        LANGUAGES.put("it", "Italian"); LANGUAGES.put("en", "English");
        LANGUAGES.put("de", "German"); LANGUAGES.put("fr", "French");
        LANGUAGES.put("es", "Spanish"); LANGUAGES.put("pt", "Portuguese");
        LANGUAGES.put("nl", "Dutch"); LANGUAGES.put("ru", "Russian");
        LANGUAGES.put("zh-Hans", "Chinese (Simpl.)"); LANGUAGES.put("zh-Hant", "Chinese (Trad.)");
        LANGUAGES.put("ja", "Japanese"); LANGUAGES.put("ko", "Korean");
        LANGUAGES.put("ar", "Arabic"); LANGUAGES.put("hi", "Hindi");
        LANGUAGES.put("pl", "Polish"); LANGUAGES.put("tr", "Turkish");
        LANGUAGES.put("sv", "Swedish"); LANGUAGES.put("da", "Danish");
        LANGUAGES.put("fi", "Finnish"); LANGUAGES.put("no", "Norwegian");
        LANGUAGES.put("cs", "Czech"); LANGUAGES.put("el", "Greek");
        LANGUAGES.put("he", "Hebrew"); LANGUAGES.put("hu", "Hungarian");
        LANGUAGES.put("id", "Indonesian"); LANGUAGES.put("ro", "Romanian");
        LANGUAGES.put("th", "Thai"); LANGUAGES.put("uk", "Ukrainian");
        LANGUAGES.put("vi", "Vietnamese");
    }
    // Android TTS language (Locale) for each BCP-47 code
    static String androidLang(String code) {
        switch (code) {
            case "zh-Hans": return "zh";
            case "zh-Hant": return "zh";
            default: return code;
        }
    }

    // Factory preset: in-code fallback so the engine keeps talking even if the app
    // has never been opened (e.g. right after a phone reboot).
    static final String DEFAULT_ENDPOINT = "http://192.168.1.226:2027";
    static List<JSONObject> defaultModels() {
        try {
            JSONObject m = new JSONObject();
            m.put("label", "Italian (default)");
            m.put("model", "qwen3-tts-1.7b_G0");
            m.put("voice", "clone:VoceItalianaAdulta");
            m.put("lang", "it");
            List<JSONObject> out = new ArrayList<>();
            out.add(m);
            return out;
        } catch (Exception e) { return new ArrayList<>(); }
    }

    private SharedPreferences prefs;
    private TextInputLayout endpointL, apiKeyL;
    private TextInputEditText endpoint, apiKey;
    private LinearLayout modelsList;
    private TextView status;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("config", MODE_PRIVATE);
        // first launch: seed the recommended preset (empty model list)
        if (loadModels(this).isEmpty()) saveModels(this, defaultModels());
        // notification permission (Android 13+): the synthesis notification is optional
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(
                "android.permission.POST_NOTIFICATIONS") != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        buildUi();
    }

    // ---- model storage: JSON array in SharedPreferences ----
    static List<JSONObject> loadModels(android.content.Context ctx) {
        List<JSONObject> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(ctx.getSharedPreferences("config", MODE_PRIVATE).getString("models", "[]"));
            for (int i = 0; i < a.length(); i++) out.add(a.getJSONObject(i));
        } catch (Exception ignored) {}
        return out;
    }
    static void saveModels(android.content.Context ctx, List<JSONObject> models) {
        JSONArray a = new JSONArray();
        for (JSONObject m : models) a.put(m);
        ctx.getSharedPreferences("config", MODE_PRIVATE).edit().putString("models", a.toString()).apply();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.attr(this, com.google.android.material.R.attr.colorSurface));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(Ui.header(this, "tts-server-proxy",
                "Android TTS engine · Qwen3-TTS over LAN · native PCM"));

        // ---- server card ----
        MaterialCardView serverC = Ui.card(this);
        LinearLayout server = Ui.into(this, serverC);
        server.addView(Ui.sectionLabel(this, "Server settings (shared by all models)"));

        endpointL = new TextInputLayout(this, null,
                com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox);
        endpointL.setHint("Phoenix endpoint (without /v1)");
        endpoint = new TextInputEditText(endpointL.getContext());
        endpoint.setSingleLine(true);
        endpoint.setText(prefs.getString("endpoint", DEFAULT_ENDPOINT));
        endpointL.addView(endpoint);
        server.addView(endpointL, Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 0, 0, 0, Ui.dp(this, 12)));

        apiKeyL = new TextInputLayout(this, null,
                com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox);
        apiKeyL.setHint("llama-swap API key (Bearer)");
        apiKeyL.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        apiKey = new TextInputEditText(apiKeyL.getContext());
        apiKey.setSingleLine(true);
        apiKey.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        apiKey.setText(prefs.getString("api_key", ""));
        apiKeyL.addView(apiKey);
        server.addView(apiKeyL, Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 0, 0, 0, Ui.dp(this, 16)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        MaterialButton save = new MaterialButton(this); // filled
        save.setText("💾 Save configuration");
        save.setAllCaps(false);
        save.setOnClickListener(v -> {
            prefs.edit().putString("endpoint", endpoint.getText().toString().trim())
                  .putString("api_key", apiKey.getText().toString().trim()).apply();
            Toast.makeText(this, "Configuration saved", Toast.LENGTH_SHORT).show();
            status.setText("Saved. The models are available as voices in the Android TTS engine.");
        });
        row.addView(save, Ui.lp(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 0, 0, Ui.dp(this, 10), 0));
        MaterialButton test = Ui.tonal(this, "🔊 Test");
        test.setOnClickListener(v -> startActivity(new android.content.Intent(this, TestActivity.class)));
        row.addView(test, Ui.lp(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 0, 0, 0, 0));
        server.addView(row);
        root.addView(serverC);

        // ---- models card ----
        MaterialCardView modelsC = Ui.card(this);
        LinearLayout models = Ui.into(this, modelsC);
        models.addView(Ui.sectionLabel(this, "Engine voices (one Android voice per model)"));
        modelsList = new LinearLayout(this);
        modelsList.setOrientation(LinearLayout.VERTICAL);
        models.addView(modelsList);
        MaterialButton add = Ui.tonal(this, "＋ Add model");
        add.setOnClickListener(v -> editDialog(-1, null));
        LinearLayout.LayoutParams addLp = Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 0, Ui.dp(this, 12), 0, 0);
        models.addView(add, addLp);
        root.addView(modelsC);

        // ---- status ----
        status = new TextView(this);
        status.setTextSize(13);
        status.setTextColor(Ui.attr(this, com.google.android.material.R.attr.colorOnSurfaceVariant));
        status.setPadding(Ui.dp(this, 24), Ui.dp(this, 8), Ui.dp(this, 24), Ui.dp(this, 28));
        status.setText("Status: configuration loaded");
        root.addView(status);

        scroll.addView(root);
        setContentView(scroll);
        renderModels();
    }

    private void renderModels() {
        modelsList.removeAllViews();
        List<JSONObject> modelObjs = loadModels(this);
        if (modelObjs.isEmpty()) {
            TextView e = new TextView(this);
            e.setText("No models yet. Tap '＋ Add model'.");
            e.setTextSize(13);
            e.setTextColor(Ui.attr(this, com.google.android.material.R.attr.colorOnSurfaceVariant));
            e.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 8));
            modelsList.addView(e);
        }
        for (int i = 0; i < modelObjs.size(); i++) {
            final int idx = i;
            JSONObject m = modelObjs.get(i);
            MaterialCardView rowCard = new MaterialCardView(this);
            rowCard.setRadius(Ui.dp(this, 14));
            rowCard.setStrokeWidth(0);
            rowCard.setCardBackgroundColor(Ui.attr(this,
                    com.google.android.material.R.attr.colorSurfaceContainerHighest));
            rowCard.setContentPadding(Ui.dp(this, 14), Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 8));
            rowCard.setLayoutParams(Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 0, 0, 0, Ui.dp(this, 8)));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout texts = new LinearLayout(this);
            texts.setOrientation(LinearLayout.VERTICAL);
            TextView name = new TextView(this);
            String lang = m.optString("lang", "it");
            name.setText(m.optString("label", m.optString("model")));
            name.setTextSize(15);
            name.setTypeface(Typeface.DEFAULT_BOLD);
            name.setTextColor(Ui.attr(this, com.google.android.material.R.attr.colorOnSurface));
            texts.addView(name);
            TextView sub = new TextView(this);
            String langLabel = LANGUAGES.containsKey(lang) ? LANGUAGES.get(lang) : lang;
            sub.setText(langLabel + "  ·  " + (m.optString("voice").isEmpty() ? "server voice" : m.optString("voice")));
            sub.setTextSize(12);
            sub.setTextColor(Ui.attr(this, com.google.android.material.R.attr.colorOnSurfaceVariant));
            sub.setPadding(0, Ui.dp(this, 2), 0, 0);
            texts.addView(sub);
            row.addView(texts, Ui.lp(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f,
                    0, 0, 0, 0));

            MaterialButton edit = Ui.iconBtn(this, "✏️",
                    Ui.attr(this, com.google.android.material.R.attr.colorOnSurfaceVariant));
            edit.setOnClickListener(v -> editDialog(idx, m));
            row.addView(edit, Ui.lp(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, Ui.dp(this, 6), 0, Ui.dp(this, 6), 0));

            MaterialButton del = Ui.iconBtn(this, "🗑", 0xFFC62828);
            del.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Delete model")
                    .setMessage("Delete '" + m.optString("label", m.optString("model")) + "'?")
                    .setPositiveButton("Delete", (d, w) -> {
                        List<JSONObject> ms = loadModels(this); ms.remove(idx); saveModels(this, ms);
                        renderModels();
                        Toast.makeText(this, "Model deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null).show());
            row.addView(del, Ui.lp(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 0, 0, 0, 0));

            rowCard.addView(row);
            modelsList.addView(rowCard);
        }
    }

    private void editDialog(int editIndex, JSONObject existing) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(Ui.dp(this, 24), Ui.dp(this, 10), Ui.dp(this, 24), 0);

        TextInputLayout labelL = new TextInputLayout(this, null,
                com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox);
        labelL.setHint("Voice name (e.g. Italian Clara)");
        TextInputEditText labelF = new TextInputEditText(labelL.getContext());
        labelL.addView(labelF);
        form.addView(labelL, Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 0, 0, 0, Ui.dp(this, 10)));

        TextInputLayout modelL = new TextInputLayout(this, null,
                com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox);
        modelL.setHint("Model (e.g. qwen3-tts-it_G0)");
        TextInputEditText modelF = new TextInputEditText(modelL.getContext());
        modelL.addView(modelF);
        form.addView(modelL, Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 0, 0, 0, Ui.dp(this, 10)));

        TextInputLayout voiceL = new TextInputLayout(this, null,
                com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox);
        voiceL.setHint("Voice (e.g. clone:VoceItalianaAdulta, empty = server voice)");
        TextInputEditText voiceF = new TextInputEditText(voiceL.getContext());
        voiceL.addView(voiceF);
        form.addView(voiceL, Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 0, 0, 0, Ui.dp(this, 10)));

        TextView ll = new TextView(this);
        ll.setText("Language spoken by the model");
        ll.setTextSize(13);
        ll.setTextColor(Ui.attr(this, com.google.android.material.R.attr.colorOnSurfaceVariant));
        ll.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        form.addView(ll);

        final Spinner langS = new Spinner(this);
        List<String> labels = new ArrayList<>();
        List<String> codes = new ArrayList<>();
        for (Map.Entry<String, String> e : LANGUAGES.entrySet()) { codes.add(e.getKey()); labels.add(e.getValue()); }
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        langS.setAdapter(ad);

        if (existing != null) {
            labelF.setText(existing.optString("label", ""));
            modelF.setText(existing.optString("model", ""));
            voiceF.setText(existing.optString("voice", ""));
            int pos = codes.indexOf(existing.optString("lang", "it"));
            langS.setSelection(pos >= 0 ? pos : 0);
        }
        form.addView(langS, Ui.lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, 0, 0, 0, Ui.dp(this, 8)));

        String title = existing == null ? "New model" : "Edit model";
        new AlertDialog.Builder(this).setTitle(title).setView(form)
            .setPositiveButton("Save", (d, w) -> {
                try {
                    JSONObject m = existing != null ? existing : new JSONObject();
                    m.put("label", labelF.getText() == null ? "" : labelF.getText().toString().trim());
                    m.put("model", modelF.getText() == null ? "" : modelF.getText().toString().trim());
                    m.put("voice", voiceF.getText() == null ? "" : voiceF.getText().toString().trim());
                    m.put("lang", codes.get(langS.getSelectedItemPosition()));
                    List<JSONObject> ms = loadModels(this);
                    if (editIndex >= 0 && editIndex < ms.size()) ms.set(editIndex, m); else ms.add(m);
                    saveModels(this, ms);
                    renderModels();
                    Toast.makeText(this, "Model saved: " + m.optString("label"), Toast.LENGTH_SHORT).show();
                } catch (Exception e) { Toast.makeText(this, "Error: " + e, Toast.LENGTH_LONG).show(); }
            })
            .setNegativeButton("Cancel", null).show();
    }
}
