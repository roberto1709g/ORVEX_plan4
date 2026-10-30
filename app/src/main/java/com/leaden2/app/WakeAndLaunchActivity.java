package com.leaden2.app;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Transición visual mínima para solicitudes de llamada que llegan cuando ORVEX
 * está en segundo plano. Despierta la pantalla y respeta el bloqueo seguro:
 * WhatsApp solo se abre cuando Android indica que el dispositivo está desbloqueado.
 */
public class WakeAndLaunchActivity extends Activity {
    private static final String TAG = "ORVEXWakeLaunch";
    public static final String EXTRA_CONTACT_NAME = "orvex_contact_name";
    public static final String EXTRA_PHONE = "orvex_contact_phone";
    private boolean launchHandled = false;
    private boolean unlockRequestSent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestVisibleOverLockScreen();
        continueAfterUnlock();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        launchHandled = false;
        continueAfterUnlock();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Se vuelve a comprobar al regresar del diálogo de desbloqueo del sistema.
        continueAfterUnlock();
    }

    private void requestVisibleOverLockScreen() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
    }

    private void continueAfterUnlock() {
        if (launchHandled) return;

        KeyguardManager keyguard = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (keyguard == null || !keyguard.isKeyguardLocked()) {
            launchWhatsApp();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (unlockRequestSent) return;
            unlockRequestSent = true;
            keyguard.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
                @Override
                public void onDismissSucceeded() {
                    unlockRequestSent = false;
                    runOnUiThread(() -> launchWhatsApp());
                }

                @Override
                public void onDismissCancelled() {
                    unlockRequestSent = false;
                    Log.i(TAG, "El usuario canceló el desbloqueo; WhatsApp no se abrirá todavía.");
                }

                @Override
                public void onDismissError() {
                    Log.w(TAG, "Android no pudo mostrar el flujo de desbloqueo; se esperará el desbloqueo manual.");
                }
            });
        } else {
            Toast.makeText(this, "Desbloquea el teléfono para continuar con WhatsApp.", Toast.LENGTH_LONG).show();
        }
    }

    private void launchWhatsApp() {
        if (launchHandled) return;

        KeyguardManager keyguard = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (keyguard != null && keyguard.isKeyguardLocked()) {
            Log.i(TAG, "El teléfono sigue bloqueado; se esperará el desbloqueo manual.");
            return;
        }

        String phone = getIntent().getStringExtra(EXTRA_PHONE);
        String contactName = getIntent().getStringExtra(EXTRA_CONTACT_NAME);
        if (phone == null || phone.trim().isEmpty()) {
            Log.e(TAG, "Falta el número validado; no se abrirá WhatsApp.");
            finish();
            return;
        }

        String digits = phone.replaceAll("\\D", "");
        Uri[] uris = new Uri[] {
                Uri.parse("whatsapp://send?phone=" + digits),
                Uri.parse("https://wa.me/" + digits)
        };
        String[] packages = {"com.whatsapp", "com.whatsapp.w4b"};

        for (String packageName : packages) {
            for (int uriIndex = 0; uriIndex < uris.length; uriIndex++) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uris[uriIndex]);
                    intent.setPackage(packageName);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    launchHandled = true;
                    Log.i(TAG, "Chat de " + (contactName == null ? "contacto" : contactName)
                            + " solicitado mediante " + packageName + ".");
                    finish();
                    return;
                } catch (ActivityNotFoundException e) {
                    Log.i(TAG, "No se encontró manejador para " + packageName + " y URI " + uris[uriIndex]);
                } catch (SecurityException e) {
                    Log.w(TAG, "Android bloqueó la apertura de WhatsApp para " + packageName, e);
                }
            }
        }

        Toast.makeText(this, "No pude abrir WhatsApp. Comprueba que esté instalado.", Toast.LENGTH_LONG).show();
        finish();
    }
}
