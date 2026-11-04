package it.ttsserverproxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Riporta il servizio FGS in piedi dopo un riavvio o un update dell'app.
 *  Il TextToSpeechService e' solo "bound": il framework lo crea al primo bind e
 *  muore col processo; se la ROM uccide il processo durante l'inattivita', il
 *  bind successivo ricrea il servizio CON l'app in background e lo startForeground
 *  di onCreate viene bloccato dalle FGS-launch restrictions (Android 12+) →
 *  processo cached → freezer → "Speech engine problem".
 *  Un FGS started da BOOT_COMPLETED (esenzione esplicita alle restrictions) +
 *  START_STICKY (riavvio automatico del sistema dopo ogni kill) rende il motore
 *  permanentemente foreground: i bind si agganciano a un servizio gia' vivo. */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context ctx, Intent i) {
        String a = i == null || i.getAction() == null ? "" : i.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(a) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) {
            try {
                java.io.File f = new java.io.File(ctx.getFilesDir(), "debug.log");
                java.io.FileWriter w = new java.io.FileWriter(f, true);
                w.write(System.currentTimeMillis() + " BootReceiver " + a + " -> startForegroundService\n");
                w.close();
                ctx.startForegroundService(new Intent(ctx, LanTtsService.class));
            } catch (Throwable t) {
                try {
                    java.io.File f = new java.io.File(ctx.getFilesDir(), "debug.log");
                    java.io.FileWriter w = new java.io.FileWriter(f, true);
                    w.write(System.currentTimeMillis() + " BootReceiver ERR: " + t + "\n");
                    w.close();
                } catch (Exception ignored) {}
            }
        }
    }
}
