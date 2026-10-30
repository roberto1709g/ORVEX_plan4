package com.leaden2.app;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Detecta mensajes visibles de WhatsApp sin modificar el servicio de accesibilidad
 * que ya controla la automatización de llamadas.
 */
public class WhatsAppNotificationListenerService extends NotificationListenerService {
    private static final String TAG = "ORVEXWhatsAppNotif";
    private static final String PREFS = "leaden2_local_data";
    private static final long DEDUPE_WINDOW_MS = 30_000L;
    private final Map<String, Long> processed = new HashMap<>();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !isAssistantEnabled()) return;

        String pkg = sbn.getPackageName();
        if (!"com.whatsapp".equals(pkg) && !"com.whatsapp.w4b".equals(pkg)) return;

        Notification notification = sbn.getNotification();
        if (notification == null) return;
        Bundle extras = notification.extras;
        if (extras == null) return;

        // Evita resúmenes de grupos y notificaciones sin contenido útil.
        if ((notification.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
        if ((notification.flags & Notification.FLAG_ONGOING_EVENT) != 0) return;

        CharSequence titleCs = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence textCs = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (TextUtils.isEmpty(textCs)) textCs = extras.getCharSequence(Notification.EXTRA_TEXT);
        if (TextUtils.isEmpty(textCs)) return;

        String sender = titleCs == null ? "un contacto" : titleCs.toString().trim();
        String text = textCs.toString().trim();
        if (text.isEmpty()) return;

        // Evita reproducir el mismo evento varias veces cuando WhatsApp actualiza la notificación.
        String key = pkg + "|" + sbn.getId() + "|" + (sbn.getTag() == null ? "" : sbn.getTag()) + "|" + sender + "|" + text;
        long now = System.currentTimeMillis();
        synchronized (processed) {
            Long previous = processed.get(key);
            if (previous != null && now - previous < DEDUPE_WINDOW_MS) return;
            processed.put(key, now);
            if (processed.size() > 100) processed.clear();
        }

        Log.i(TAG, "Mensaje WhatsApp detectado; remitente=" + sender + ", caracteres=" + text.length());

        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(PlaybackService.ACTION_WHATSAPP_ALERT);
        intent.putExtra(PlaybackService.EXTRA_WHATSAPP_SENDER, sender);
        intent.putExtra(PlaybackService.EXTRA_WHATSAPP_TEXT, text);
        try {
            startService(intent);
        } catch (Exception e) {
            Log.e(TAG, "No se pudo solicitar el aviso de WhatsApp.", e);
        }
    }

    private boolean isAssistantEnabled() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean("assistant_enabled", false);
    }

    /** Utilidad para que MainActivity pueda mostrar el estado del permiso. */
    public static boolean isEnabled(android.content.Context context) {
        try {
            String enabled = Settings.Secure.getString(
                context.getContentResolver(),
                "enabled_notification_listeners"
            );
            String component = context.getPackageName() + "/" + WhatsAppNotificationListenerService.class.getName();
            return enabled != null && enabled.toLowerCase().contains(component.toLowerCase());
        } catch (Exception e) {
            return false;
        }
    }
}
