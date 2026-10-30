package com.leaden2.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import android.graphics.Rect;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Automatización opcional y local de la interfaz de WhatsApp.
 * Solo actúa después de que ORVEX haya recibido un “sí” y armado el número validado.
 * Si no puede verificar el chat o identificar con claridad el control de llamada de voz,
 * no pulsa nada.
 */
public class WhatsAppCallAccessibilityService extends AccessibilityService {
    private static final String TAG = "ORVEXWaAccess";
    private static final String PREFS = "leaden2_local_data";
    private static final long MAX_ARMED_MS = 90000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean checkQueued = false;
    private boolean diagWindowShown = false;
    private boolean diagContactShown = false;
    private boolean diagButtonShown = false;
    private View diagnosticOverlay;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                    | AccessibilityEvent.TYPE_WINDOWS_CHANGED;
            // Android espera nombres de paquete como arreglo; evitamos depender
            // de cómo se interprete la lista declarada en el XML.
            info.packageNames = new String[] {"com.whatsapp", "com.whatsapp.w4b"};
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                    | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            info.notificationTimeout = 250;
            setServiceInfo(info);
        }
        Log.i(TAG, "Servicio de accesibilidad habilitado por el usuario.");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String packageName = event.getPackageName().toString();
        if (!"com.whatsapp".equals(packageName) && !"com.whatsapp.w4b".equals(packageName)) return;
        scheduleInspection(500);
    }

    /**
     * Reintenta de forma limitada mientras la orden siga armada. WhatsApp puede tardar
     * en terminar de cargar el chat y el árbol de accesibilidad no siempre está listo
     * en el primer evento de ventana.
     */
    private void scheduleInspection(long delayMs) {
        if (checkQueued) return;
        checkQueued = true;
        handler.postDelayed(() -> {
            checkQueued = false;
            inspectWhatsAppWindow();
        }, delayMs);
    }

    private void inspectWhatsAppWindow() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean("wa_auto_call_armed", false)) {
            diagWindowShown = false;
            diagContactShown = false;
            diagButtonShown = false;
            prefs.edit().putBoolean("wa_call_menu_opened", false).apply();
            return;
        }

        long armedAt = prefs.getLong("wa_auto_call_armed_at", 0L);
        if (armedAt == 0L || System.currentTimeMillis() - armedAt > MAX_ARMED_MS) {
            disarm("Se agotó el tiempo de espera; no se inició la llamada.");
            return;
        }

        String expectedName = prefs.getString("wa_auto_call_name", "");
        String expectedPhone = digits(prefs.getString("wa_auto_call_phone", ""));
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null && !diagWindowShown) {
            CharSequence rootPackage = root.getPackageName();
            showDiagnostic("Diagnóstico ORVEX: ventana " + (rootPackage == null ? "sin paquete" : rootPackage));
            diagWindowShown = true;
        }
        if (expectedPhone.isEmpty()) {
            Log.w(TAG, "La orden está armada pero no hay número esperado; no se pulsará nada.");
            disarm("No hay número validado para la llamada.");
            return;
        }
        if (root == null) {
            Log.d(TAG, "WhatsApp aún no ofrece una ventana accesible; se reintentará.");
            scheduleInspection(900);
            return;
        }

        boolean menuOpened = prefs.getBoolean("wa_call_menu_opened", false);
        if (menuOpened) {
            // La identidad del contacto ya fue comprobada antes de abrir este menú.
            // No se vuelve a exigir que el árbol del menú contenga la cabecera del chat:
            // WhatsApp puede exponer el menú como una ventana accesible independiente.
            AccessibilityNodeInfo optionNode = findVoiceCallMenuOptionAcrossWindows(root);
            if (optionNode == null) {
                if (!diagButtonShown) {
                    showDiagnostic("ORVEX: menú de llamadas abierto; buscando la opción exacta «Llamada».");
                    diagButtonShown = true;
                }
                Log.d(TAG, "El menú está abierto, pero la opción de voz no aparece en el árbol accesible; se reintentará sin pulsar.");
                scheduleInspection(700);
                return;
            }

            boolean clicked = clickSelfOrClickableParent(optionNode);
            // Desarmar siempre después de un intento para impedir llamadas repetidas.
            prefs.edit().putBoolean("wa_auto_call_armed", false)
                    .putBoolean("wa_call_menu_opened", false).apply();
            Log.i(TAG, clicked
                    ? "Se seleccionó la opción textual exacta de llamada de voz para el contacto previamente validado."
                    : "Se identificó la opción de llamada de voz, pero Android rechazó la acción.");
            showDiagnostic(clicked
                    ? "ORVEX: se seleccionó «Llamada». Comprueba que WhatsApp inició la llamada."
                    : "ORVEX: Android no aceptó la selección de «Llamada»; no se repetirá.");
            return;
        }

        if (!containsExpectedContact(root, expectedName, expectedPhone)) {
            if (!diagContactShown) {
                showDiagnostic("ORVEX: esperando verificar el contacto en WhatsApp; no se ha pulsado nada.");
                diagContactShown = true;
            }
            Log.d(TAG, "El contacto visible aún no coincide con el contacto esperado; se reintentará sin pulsar.");
            scheduleInspection(900);
            return;
        }
        if (!diagContactShown) {
            showDiagnostic("Diagnóstico ORVEX: contacto verificado.");
            diagContactShown = true;
        }

        AccessibilityNodeInfo callNode = findHeaderCallControl(root);
        if (callNode == null) {
            if (!diagButtonShown) {
                showDiagnostic("ORVEX: contacto verificado; esperando el icono de llamadas de WhatsApp.");
                diagButtonShown = true;
            }
            Log.d(TAG, "El contacto coincide, pero aún no se identifica el icono de llamadas de la cabecera; se reintentará sin pulsar.");
            scheduleInspection(700);
            return;
        }

        // Primera etapa: abrir el menú de WhatsApp. La orden sigue armada para la segunda etapa.
        boolean clicked = clickSelfOrClickableParent(callNode);
        if (clicked) {
            prefs.edit().putBoolean("wa_call_menu_opened", true).apply();
            showDiagnostic("ORVEX: menú de llamadas solicitado; buscaré la opción «Llamada».");
            Log.i(TAG, "Se pulsó una vez el icono de llamadas de la cabecera; se buscará la opción de voz del menú.");
            scheduleInspection(700);
        } else {
            prefs.edit().putBoolean("wa_auto_call_armed", false)
                    .putBoolean("wa_call_menu_opened", false).apply();
            showDiagnostic("ORVEX: Android rechazó abrir el menú de llamadas; no se repetirá.");
            Log.w(TAG, "Android rechazó el toque al icono de llamadas; operación desarmada para evitar repeticiones.");
        }
    }

    private boolean containsExpectedContact(AccessibilityNodeInfo root, String expectedName, String expectedPhone) {
        HeaderEvidence evidence = new HeaderEvidence();
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        collectHeaderEvidence(root, normalize(expectedName), expectedPhone, screenHeight, evidence);

        // Un número explícitamente visible y distinto invalida la coincidencia del nombre.
        // Si WhatsApp oculta el número en la cabecera, se permite el primer nombre porque
        // el chat se abrió previamente con el número validado de la agenda ORVEX.
        if (evidence.phoneMismatch) {
            Log.w(TAG, "Cabecera rechazada: WhatsApp expone un número distinto al número solicitado.");
            return false;
        }
        boolean verified = evidence.phoneMatch || evidence.firstNameMatch;
        if (verified) {
            Log.i(TAG, evidence.phoneMatch
                    ? "Contacto verificado por número visible coincidente."
                    : "Contacto verificado por primer nombre en la cabecera visible; el chat fue abierto por número.");
        }
        return verified;
    }

    private static final class HeaderEvidence {
        boolean firstNameMatch;
        boolean phoneMatch;
        boolean phoneMismatch;
    }

    private void collectHeaderEvidence(AccessibilityNodeInfo node, String expectedName,
                                       String expectedPhone, int screenHeight, HeaderEvidence evidence) {
        if (node == null) return;
        if (node.isVisibleToUser()) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            boolean inHeader = bounds.top >= 0 && bounds.top < (int) (screenHeight * 0.30f)
                    && bounds.bottom > bounds.top;
            if (inHeader) {
                recordHeaderValue(node.getText(), expectedName, expectedPhone, evidence);
                recordHeaderValue(node.getContentDescription(), expectedName, expectedPhone, evidence);
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectHeaderEvidence(node.getChild(i), expectedName, expectedPhone, screenHeight, evidence);
        }
    }

    private void recordHeaderValue(CharSequence value, String expectedName, String expectedPhone,
                                   HeaderEvidence evidence) {
        if (value == null) return;
        String visibleText = value.toString();
        String normalized = normalize(visibleText);
        String allDigits = digits(visibleText);
        boolean containsMobile = allDigits.matches(".*09\\d{8}.*")
                || allDigits.matches(".*5939\\d{8}.*");
        String expectedDigits = canonicalEcuadorPhone(expectedPhone);

        if (containsMobile) {
            String visibleDigits = canonicalEcuadorPhone(visibleText);
            if (!expectedDigits.isEmpty() && visibleDigits.equals(expectedDigits)) {
                evidence.phoneMatch = true;
            } else {
                evidence.phoneMismatch = true;
            }
            return;
        }

        String expectedFirstName = firstWord(expectedName);
        if (!expectedFirstName.isEmpty()
                && (normalized.equals(expectedFirstName)
                || normalized.startsWith(expectedFirstName + " "))) {
            evidence.firstNameMatch = true;
        }
    }

    private String firstWord(String normalizedName) {
        if (normalizedName == null || normalizedName.trim().isEmpty()) return "";
        String[] words = normalizedName.trim().split("\\s+");
        return words.length == 0 ? "" : words[0];
    }

    /**
     * Compara teléfonos móviles ecuatorianos en sus formas habitual local (09...) e
     * internacional (5939...). La comparación solo se usa en el encabezado visible.
     */
    private String canonicalEcuadorPhone(String value) {
        String d = digits(value);
        if (d.startsWith("593") && d.length() >= 12) d = d.substring(3);
        if (d.startsWith("0") && d.length() >= 10) d = d.substring(1);
        if (d.length() > 9) d = d.substring(d.length() - 9);
        return d;
    }

    /** Encuentra únicamente el control de llamadas de la cabecera, no una opción del menú. */
    private AccessibilityNodeInfo findHeaderCallControl(AccessibilityNodeInfo root) {
        if (root == null) return null;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        return findHeaderCallControlRecursive(root, screenHeight);
    }

    private AccessibilityNodeInfo findHeaderCallControlRecursive(AccessibilityNodeInfo node, int screenHeight) {
        if (node == null) return null;
        String viewId = node.getViewIdResourceName();
        String desc = node.getContentDescription() == null ? "" : normalize(node.getContentDescription().toString());
        String idLower = viewId == null ? "" : viewId.toLowerCase(Locale.ROOT);
        boolean video = desc.contains("video") || desc.contains("videollamada")
                || idLower.contains("video_call") || idLower.contains("video-call");
        boolean idLooksLikeCall = !idLower.isEmpty()
                && (idLower.contains("call") || idLower.contains("voice")) && !video;
        boolean descriptionLooksLikeCall = !video && (
                desc.equals("llamada de voz") || desc.equals("llamar") || desc.equals("llamada")
                || desc.equals("llamada de voz por whatsapp") || desc.equals("voice call")
                || desc.equals("call") || desc.equals("make a call") || desc.equals("start voice call"));
        if (node.isVisibleToUser() && node.isEnabled()) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            boolean inHeader = bounds.top >= 0 && bounds.top < (int)(screenHeight * 0.35f)
                    && bounds.bottom > bounds.top;
            if (inHeader && (idLooksLikeCall || descriptionLooksLikeCall)) return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findHeaderCallControlRecursive(node.getChild(i), screenHeight);
            if (found != null) return found;
        }
        return null;
    }

    /**
     * Busca la opción de voz en la ventana activa y en las ventanas accesibles de WhatsApp.
     * Esto contempla menús emergentes que no pertenecen al árbol de la cabecera del chat.
     */
    private AccessibilityNodeInfo findVoiceCallMenuOptionAcrossWindows(AccessibilityNodeInfo activeRoot) {
        AccessibilityNodeInfo found = findVoiceCallMenuOption(activeRoot);
        if (found != null) return found;
        try {
            for (AccessibilityWindowInfo window : getWindows()) {
                if (window == null) continue;
                AccessibilityNodeInfo windowRoot = window.getRoot();
                if (windowRoot == null) continue;
                CharSequence packageName = windowRoot.getPackageName();
                if (packageName != null && !"com.whatsapp".contentEquals(packageName)
                        && !"com.whatsapp.w4b".contentEquals(packageName)) continue;
                found = findVoiceCallMenuOption(windowRoot);
                if (found != null) return found;
            }
        } catch (Exception e) {
            Log.w(TAG, "No fue posible inspeccionar todas las ventanas accesibles de WhatsApp.", e);
        }
        return null;
    }

    /** Busca texto visible exacto del menú; excluye videollamada y enlace de llamada. */
    private AccessibilityNodeInfo findVoiceCallMenuOption(AccessibilityNodeInfo root) {
        if (root == null) return null;
        return findVoiceCallMenuOptionRecursive(root);
    }

    private boolean isExactVoiceCallLabel(String value) {
        return value.equals("llamada") || value.equals("llamada de voz")
                || value.equals("llamar") || value.equals("voice call")
                || value.equals("call") || value.equals("start voice call");
    }

    private AccessibilityNodeInfo findVoiceCallMenuOptionRecursive(AccessibilityNodeInfo node) {
        if (node == null) return null;
        String text = node.getText() == null ? "" : normalize(node.getText().toString());
        String desc = node.getContentDescription() == null ? "" : normalize(node.getContentDescription().toString());
        boolean exactVoiceOption = isExactVoiceCallLabel(text) || isExactVoiceCallLabel(desc);
        boolean forbidden = text.contains("video") || text.contains("videollamada")
                || text.contains("enlace") || text.contains("link")
                || desc.contains("video") || desc.contains("videollamada")
                || desc.contains("enlace") || desc.contains("link");
        if (node.isVisibleToUser() && node.isEnabled() && exactVoiceOption && !forbidden) {
            Log.i(TAG, "Opción de menú de llamada localizada mediante texto exacto: " + text);
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findVoiceCallMenuOptionRecursive(node.getChild(i));
            if (found != null) return found;
        }
        return null;
    }

    private boolean clickSelfOrClickableParent(AccessibilityNodeInfo node) {
        // Primer intento: acción semántica de accesibilidad sobre el elemento o su fila.
        AccessibilityNodeInfo current = node;
        Rect gestureBounds = new Rect();
        for (int depth = 0; current != null && depth < 5; depth++) {
            if (current.isVisibleToUser() && current.isEnabled() && current.isClickable()) {
                if (current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
            }
            // Conserva solo límites reales y razonables del texto/fila; no toca coordenadas fijas.
            Rect candidate = new Rect();
            current.getBoundsInScreen(candidate);
            int screenW = getResources().getDisplayMetrics().widthPixels;
            int screenH = getResources().getDisplayMetrics().heightPixels;
            boolean valid = current.isVisibleToUser() && candidate.left >= 0 && candidate.top >= 0
                    && candidate.right <= screenW && candidate.bottom <= screenH
                    && candidate.width() >= 24 && candidate.height() >= 24
                    && candidate.height() <= Math.max(180, (int)(screenH * 0.22f));
            if (valid && gestureBounds.isEmpty()) gestureBounds.set(candidate);
            current = current.getParent();
        }
        // Respaldo: gesto en el centro de los límites que Android expone para la opción exacta.
        // El XML declara canPerformGestures=true; sin esa capacidad dispatchGesture puede fallar.
        if (!gestureBounds.isEmpty()) {
            final float x = gestureBounds.exactCenterX();
            final float y = gestureBounds.exactCenterY();
            Path path = new Path();
            path.moveTo(x, y);
            path.lineTo(x, y);
            GestureDescription.StrokeDescription stroke =
                    new GestureDescription.StrokeDescription(path, 0, 100);
            GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
            boolean dispatched = dispatchGesture(gesture, new GestureResultCallback() {
                @Override public void onCompleted(GestureDescription gestureDescription) {
                    Log.i(TAG, "Gesto de selección enviado sobre los límites accesibles de la opción de llamada de voz.");
                }
                @Override public void onCancelled(GestureDescription gestureDescription) {
                    Log.w(TAG, "Android canceló el gesto de selección de llamada de voz.");
                }
            }, handler);
            if (dispatched) {
                Log.i(TAG, "Android aceptó el gesto sobre una opción con límites accesibles válidos: " + gestureBounds);
                return true;
            }
            Log.w(TAG, "dispatchGesture rechazó el gesto; límites calculados: " + gestureBounds);
        } else {
            Log.w(TAG, "No hay límites accesibles válidos para aplicar un gesto seguro a la opción de llamada.");
        }
        return false;
    }

    private void showDiagnostic(String message) {
        // Los mensajes de diagnóstico se registran en Logcat, no se muestran como
        // Toasts durante la automatización: la interfaz de WhatsApp debe permanecer
        // libre para que el servicio pueda inspeccionar el chat y el menú.
        Log.i(TAG, message);
    }

    /**
     * Diagnóstico visible persistente: evita depender de un Toast que desaparece.
     * Muestra los textos, descripciones, IDs y posiciones que Android expone en la
     * cabecera de WhatsApp. No realiza ninguna acción de llamada.
     */
    private void showHeaderDiagnostic(AccessibilityNodeInfo root, String expectedName, String expectedPhone) {
        StringBuilder report = new StringBuilder();
        report.append("ORVEX — DIAGNÓSTICO DE WHATSAPP\n\n");
        report.append("Contacto esperado: ").append(expectedName).append("\n");
        report.append("Número esperado: ").append(expectedPhone).append("\n");
        CharSequence pkg = root == null ? null : root.getPackageName();
        report.append("Paquete de ventana: ").append(pkg == null ? "desconocido" : pkg).append("\n\n");
        report.append("ELEMENTOS ACCESIBLES EN LA PARTE SUPERIOR:\n");
        int height = getResources().getDisplayMetrics().heightPixels;
        int[] count = new int[]{0};
        appendHeaderNodes(root, height, report, count, 0);
        if (count[0] == 0) {
            report.append("Android no expuso texto ni controles accesibles en el 40% superior de esta ventana.\n");
        }
        report.append("\nNo se ha pulsado ningún botón. Cierra este panel para volver a WhatsApp.");
        final String content = report.toString();
        Log.i(TAG, content);
        handler.post(() -> showDiagnosticOverlay(content));
    }

    private void appendHeaderNodes(AccessibilityNodeInfo node, int screenHeight,
                                   StringBuilder report, int[] count, int depth) {
        if (node == null || depth > 30 || count[0] >= 45) return;
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        boolean topArea = bounds.top >= 0 && bounds.top < (int)(screenHeight * 0.40f)
                && bounds.bottom > bounds.top;
        String text = node.getText() == null ? "" : node.getText().toString().trim();
        String desc = node.getContentDescription() == null ? "" : node.getContentDescription().toString().trim();
        String id = node.getViewIdResourceName() == null ? "" : node.getViewIdResourceName();
        if (topArea && (!text.isEmpty() || !desc.isEmpty() || !id.isEmpty())) {
            report.append("• ").append(node.getClassName() == null ? "vista" : node.getClassName())
                    .append(" [").append(bounds.left).append(',').append(bounds.top).append('-')
                    .append(bounds.right).append(',').append(bounds.bottom).append("]\n");
            if (!text.isEmpty()) report.append("  texto: ").append(shorten(text, 180)).append("\n");
            if (!desc.isEmpty()) report.append("  descripción: ").append(shorten(desc, 180)).append("\n");
            if (!id.isEmpty()) report.append("  ID: ").append(id).append("\n");
            report.append("  clicable=").append(node.isClickable()).append(", habilitado=")
                    .append(node.isEnabled()).append("\n\n");
            count[0]++;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            appendHeaderNodes(node.getChild(i), screenHeight, report, count, depth + 1);
            if (count[0] >= 45) break;
        }
    }

    private String shorten(String value, int max) {
        if (value == null) return "";
        String oneLine = value.replace('\n', ' ').replace('\r', ' ').trim();
        return oneLine.length() <= max ? oneLine : oneLine.substring(0, max) + "…";
    }

    private void showDiagnosticOverlay(String report) {
        if (diagnosticOverlay != null) return;
        try {
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            LinearLayout panel = new LinearLayout(this);
            panel.setOrientation(LinearLayout.VERTICAL);
            panel.setPadding(28, 24, 28, 20);
            panel.setBackgroundColor(Color.rgb(250, 250, 250));

            TextView title = new TextView(this);
            title.setText("Diagnóstico ORVEX");
            title.setTextSize(20);
            title.setTextColor(Color.BLACK);
            title.setPadding(0, 0, 0, 14);
            panel.addView(title, new LinearLayout.LayoutParams(-1, -2));

            ScrollView scroll = new ScrollView(this);
            TextView body = new TextView(this);
            body.setText(report);
            body.setTextSize(14);
            body.setTextColor(Color.rgb(25, 25, 25));
            body.setTextIsSelectable(true);
            scroll.addView(body, new ScrollView.LayoutParams(-1, -2));
            LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1f);
            panel.addView(scroll, scrollParams);

            Button close = new Button(this);
            close.setText("Cerrar y volver a WhatsApp");
            close.setOnClickListener(v -> {
                try { wm.removeView(panel); } catch (Exception ignored) { }
                diagnosticOverlay = null;
            });
            panel.addView(close, new LinearLayout.LayoutParams(-1, -2));

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    (int)(getResources().getDisplayMetrics().heightPixels * 0.78f),
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            wm.addView(panel, params);
            diagnosticOverlay = panel;
        } catch (Exception e) {
            Log.e(TAG, "No se pudo mostrar el panel de diagnóstico persistente.", e);
            Toast.makeText(getApplicationContext(), "No se pudo abrir el panel de diagnóstico; consulta los registros del sistema.", Toast.LENGTH_LONG).show();
        }
    }

    private void disarm(String reason) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("wa_auto_call_armed", false).apply();
        Log.w(TAG, reason);
    }

    private String normalize(String input) {
        if (input == null) return "";
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        return decomposed.replaceAll("[^a-z0-9+ ]", " ").replaceAll("\\s+", " ").trim();
    }

    private String digits(String input) {
        return input == null ? "" : input.replaceAll("\\D", "");
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "Servicio de accesibilidad interrumpido.");
    }
}
