package it.ttsserverproxy;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

/** Helper del restyling MD3: tutto costruito a codice, senza layout XML. */
final class Ui {
    private Ui() {}

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    /** Colore da attributo del tema (es. com.google.android.material.R.attr.colorSurface). */
    static int attr(Context c, int a) {
        return MaterialColors.getColor(c, a, "Ui.attr");
    }

    /** LayoutParams orizzontali/verticali con margini in dp. */
    static LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(l, t, r, b);
        return p;
    }

    /** LayoutParams con peso (righe orizzontali). */
    static LinearLayout.LayoutParams lp(int w, int h, float weight, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h, weight);
        p.setMargins(l, t, r, b);
        return p;
    }

    /** Header con gradiente blu e angoli arrotondati solo in basso. */
    static LinearLayout header(Context c, String title, String subtitle) {
        LinearLayout h = new LinearLayout(c);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setPadding(dp(c, 24), dp(c, 42), dp(c, 24), dp(c, 28));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF0D47A1, 0xFF1E88E5});
        float r = dp(c, 26);
        bg.setCornerRadii(new float[]{0, 0, 0, 0, r, r, r, r});
        h.setBackground(bg);
        TextView t = new TextView(c);
        t.setText(title);
        t.setTextSize(26);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setTextColor(0xFFFFFFFF);
        h.addView(t);
        TextView s = new TextView(c);
        s.setText(subtitle);
        s.setTextSize(13);
        s.setTextColor(0xCCFFFFFF);
        s.setPadding(0, dp(c, 4), 0, 0);
        h.addView(s);
        return h;
    }

    /** Card MD3 con angoli arrotondati, bordo sottile e padding interno. */
    static MaterialCardView card(Context c) {
        MaterialCardView card = new MaterialCardView(c);
        card.setRadius(dp(c, 20));
        card.setStrokeWidth(dp(c, 1));
        card.setStrokeColor(attr(c, com.google.android.material.R.attr.colorOutlineVariant));
        card.setCardBackgroundColor(attr(c, com.google.android.material.R.attr.colorSurfaceContainerLow));
        card.setContentPadding(dp(c, 18), dp(c, 16), dp(c, 18), dp(c, 18));
        card.setCardElevation(0);
        card.setLayoutParams(lp(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 12)));
        return card;
    }

    /** Bottoni MD3: il tema non espone gli attr *Style per costruttore, quindi
     *  parto dal filled di default e tinto manualmente container/contorno. */
    static MaterialButton tonal(Context c, String text) {
        MaterialButton b = new MaterialButton(c);
        b.setText(text);
        b.setAllCaps(false);
        b.setBackgroundTintList(ColorStateList.valueOf(
                attr(c, com.google.android.material.R.attr.colorSecondaryContainer)));
        b.setTextColor(attr(c, com.google.android.material.R.attr.colorOnSecondaryContainer));
        return b;
    }

    static MaterialButton outlined(Context c, String text, int textColor) {
        MaterialButton b = new MaterialButton(c);
        b.setText(text);
        b.setAllCaps(false);
        b.setBackgroundTintList(ColorStateList.valueOf(0x00000000));
        b.setStrokeWidth(dp(c, 1));
        b.setStrokeColor(ColorStateList.valueOf(
                attr(c, com.google.android.material.R.attr.colorOutline)));
        b.setTextColor(textColor);
        return b;
    }

    /** Pulsante compatto a glifo (azioni di riga tipo edit/elimina). */
    static MaterialButton iconBtn(Context c, String glyph, int textColor) {
        MaterialButton b = outlined(c, glyph, textColor);
        b.setHeight(dp(c, 38));
        b.setMinimumWidth(0);
        b.setMinWidth(0);
        b.setPadding(dp(c, 10), 0, dp(c, 10), 0);
        b.setTextSize(13);
        return b;
    }

    /** Colonna verticale dentro una card: MaterialCardView e' un FrameLayout
     *  e sovrappone i figli, quindi il contenuto va impilato a mano qui. */
    static LinearLayout into(Context c, MaterialCardView card) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        card.addView(col, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return col;
    }

    /** Etichetta di sezione (maiuscoletto colorato) dentro una card. */
    static TextView sectionLabel(Context c, String text) {
        TextView l = new TextView(c);
        l.setText(text.toUpperCase());
        l.setTextSize(11);
        l.setTypeface(Typeface.DEFAULT_BOLD);
        l.setLetterSpacing(0.08f);
        l.setTextColor(attr(c, com.google.android.material.R.attr.colorPrimary));
        l.setPadding(0, 0, 0, dp(c, 10));
        return l;
    }
}
