package com.leaden2.app;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AiPlanActivity extends AppCompatActivity {
    public static final String EXTRA_FEATURE = "orvex_feature";
    public static final String FEATURE_PLAN = "plan_ia";
    public static final String FEATURE_ANALYSIS = "analisis";
    private static final String TAG = "ORVEX_ACCESS_VALIDATION";
    private static final String PLAN_WEBHOOK =
        "https://n8n.serviapps.duckdns.org/webhook/ORVEX_plan";

    private WebView webView;
    private String requestedFeature = FEATURE_PLAN;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean requestInProgress = new AtomicBoolean(false);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String feature = getIntent().getStringExtra(EXTRA_FEATURE);
        if (FEATURE_ANALYSIS.equals(feature)) {
            requestedFeature = FEATURE_ANALYSIS;
        }

        webView = new WebView(this);
        webView.setBackgroundColor(0xFF070B14);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new PlanBridge(), "OrvexPlan");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/plan_ia.html");
    }

    private final class PlanBridge {
        @JavascriptInterface
        public String getFeatureName() {
            return FEATURE_ANALYSIS.equals(requestedFeature)
                ? "COMANDO DE ANÁLISIS"
                : "PLAN DE INTELIGENCIA ARTIFICIAL";
        }

        @JavascriptInterface
        public void validateCode(String rawCode) {
            final String code = rawCode == null ? "" : rawCode.trim();
            if (!code.matches("\\d{6}")) {
                notifyPage(false, "Ingresa un código numérico de exactamente 6 dígitos.");
                return;
            }
            if (!requestInProgress.compareAndSet(false, true)) return;

            executor.execute(() -> {
                HttpURLConnection connection = null;
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("codigo", code);
                    payload.put("switch", requestedFeature);

                    URL url = new URL(PLAN_WEBHOOK);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(20000);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    connection.setRequestProperty("Accept", "application/json");
                    byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
                    connection.setFixedLengthStreamingMode(body.length);
                    try (OutputStream output = connection.getOutputStream()) {
                        output.write(body);
                    }

                    int status = connection.getResponseCode();
                    InputStream stream = status >= 200 && status < 300
                        ? connection.getInputStream() : connection.getErrorStream();
                    String response = readResponse(stream);
                    boolean valid = status >= 200 && status < 300 && responseMeansTrue(response);

                    if (valid) {
                        SharedPreferences.Editor preferences = getSharedPreferences(
                            "leaden2_local_data", MODE_PRIVATE).edit();
                        if (FEATURE_ANALYSIS.equals(requestedFeature)) {
                            preferences.putBoolean("analysis_code_validated", true);
                            preferences.putBoolean("analysis_command_enabled", true);
                            preferences.apply();
                            Log.i(TAG, "Código del comando de análisis validado por n8n.");
                            notifyPage(true, "Código válido. Comando de análisis activado.");
                        } else {
                            preferences.putBoolean("ai_plan_enabled", true).apply();
                            Log.i(TAG, "Código del plan de inteligencia artificial validado por n8n.");
                            notifyPage(true, "Código válido. Plan de inteligencia artificial activado.");
                        }
                    } else {
                        Log.w(TAG, "Código rechazado para " + requestedFeature + ". HTTP=" + status);
                        notifyPage(false, "El código ingresado no es válido. Verifica los seis dígitos e inténtalo nuevamente.");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "No se pudo validar el código del plan.", e);
                    notifyPage(false, "No fue posible validar el código. Comprueba tu conexión e inténtalo nuevamente.");
                } finally {
                    if (connection != null) connection.disconnect();
                    requestInProgress.set(false);
                }
            });
        }

        @JavascriptInterface
        public void goBack() {
            runOnUiThread(() -> {
                setResult(RESULT_CANCELED);
                finish();
            });
        }
    }

    private void notifyPage(boolean valid, String message) {
        runOnUiThread(() -> {
            if (webView == null) return;
            String safeMessage = JSONObject.quote(message);
            webView.evaluateJavascript(
                "window.onPlanResult(" + valid + ", " + safeMessage + ");", null);
            if (valid) {
                mainHandler.postDelayed(() -> {
                    setResult(RESULT_OK);
                    finish();
                }, 900);
            }
        });
    }

    private boolean responseMeansTrue(String response) {
        if (response == null) return false;
        String value = response.trim().replace("\\uFEFF", "");
        if ("true".equalsIgnoreCase(value) || "\"true\"".equalsIgnoreCase(value)) return true;
        try {
            Object parsed = new JSONTokener(value).nextValue();
            if (parsed instanceof Boolean) return (Boolean) parsed;
            if (parsed instanceof JSONObject) {
                JSONObject object = (JSONObject) parsed;
                return object.optBoolean("valid", false)
                    || object.optBoolean("success", false)
                    || object.optBoolean("resultado", false)
                    || object.optBoolean("authorized", false);
            }
        } catch (Exception ignored) { }
        return false;
    }

    private String readResponse(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
        }
        return result.toString();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("OrvexPlan");
            webView.destroy();
            webView = null;
        }
        executor.shutdownNow();
        super.onDestroy();
    }
}
