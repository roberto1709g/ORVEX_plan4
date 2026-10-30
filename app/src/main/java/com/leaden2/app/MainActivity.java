package com.leaden2.app;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_RECORD_AUDIO = 410;
    private static final int REQUEST_AI_PLAN = 420;
    private static final int REQUEST_ANALYSIS_PLAN = 421;
    private static final String PREFS = "leaden2_local_data";
    private static final String KEY_PROFILE = "profile";
    private static final String KEY_CONTACTS = "contacts";
    private static final int COLOR_BG = Color.rgb(4, 9, 18);
    private static final int COLOR_PANEL = Color.rgb(10, 20, 34);
    private static final int COLOR_CYAN = Color.rgb(0, 229, 255);
    private static final int COLOR_TEXT = Color.rgb(230, 246, 255);
    private static final int COLOR_MUTED = Color.rgb(133, 166, 188);
    private static final int COLOR_EXIT = Color.rgb(255, 93, 112);

    private EditText nombreInput, cedulaInput, celularInput, correoInput;
    private EditText contactoNombreInput, contactoTelefonoInput;
    private TextView status, contactsView;
    private SwitchCompat assistantSwitch;
    private SwitchCompat whatsappAutoReadSwitch;
    private SwitchCompat analysisSwitch;
    private SwitchCompat aiPlanSwitch;
    private Button testButton;
    private boolean suppressSwitchListener = false;
    private JSONArray contacts = new JSONArray();
    private SharedPreferences prefs;
    private String pendingPermissionAction = VoiceAssistantService.ACTION_PREPARE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadLocalData();
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(30));
        root.setBackgroundColor(COLOR_BG);
        scroll.addView(root);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(dp(18), dp(22), dp(18), dp(20));
        hero.setBackground(panelBackground(COLOR_PANEL, COLOR_CYAN, 20, 1));
        root.addView(hero, buttonParams());

        TextView systemLabel = new TextView(this);
        systemLabel.setText("●  SISTEMA DE ASISTENCIA PERSONAL");
        systemLabel.setTextColor(COLOR_CYAN);
        systemLabel.setTextSize(10);
        systemLabel.setLetterSpacing(0.12f);
        systemLabel.setGravity(Gravity.CENTER);
        hero.addView(systemLabel, matchWrap());

        TextView title = new TextView(this);
        title.setText("ORVEX");
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(34);
        title.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        title.setLetterSpacing(0.22f);
        title.setGravity(Gravity.CENTER);
        title.setShadowLayer(dp(12), 0, 0, COLOR_CYAN);
        title.setPadding(0, dp(8), 0, dp(4));
        hero.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("INTERFAZ DE CONTROL  //  V1.4");
        subtitle.setTextColor(COLOR_MUTED);
        subtitle.setTextSize(12);
        subtitle.setLetterSpacing(0.08f);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, dp(2));
        hero.addView(subtitle, matchWrap());

        addSection(root, "1. DATOS DEL USUARIO");
        nombreInput = addField(root, "Nombre completo", "Nombre y apellidos");
        cedulaInput = addField(root, "Cédula", "Número de identificación");
        celularInput = addField(root, "Celular", "Número de teléfono");
        correoInput = addField(root, "Correo electrónico", "usuario@correo.com");

        Button save = makeButton("GUARDAR REGISTRO LOCAL");
        save.setOnClickListener(v -> saveProfile(true));
        root.addView(save, buttonParams());

        addSection(root, "2. CONTACTOS DE ORVEX");
        contactoNombreInput = addField(root, "Nombre del contacto", "Ej. Roberto");
        contactoTelefonoInput = addField(root, "Teléfono del contacto", "Ej. 0991234567");
        Button addContact = makeButton("AGREGAR CONTACTO");
        addContact.setOnClickListener(v -> addContact());
        root.addView(addContact, buttonParams());

        contactsView = new TextView(this);
        contactsView.setTextColor(Color.WHITE);
        contactsView.setTextSize(14);
        contactsView.setTextColor(COLOR_TEXT);
        contactsView.setPadding(dp(14), dp(14), dp(14), dp(14));
        contactsView.setBackground(panelBackground(COLOR_PANEL, Color.rgb(26, 68, 88), 14, 1));
        root.addView(contactsView, buttonParams());
        renderContacts();

        addSection(root, "3. BIBLIOTECA DE AUDIOS");
        Button audioLibraryButton = makeButton("ABRIR REPRODUCTOR DE AUDIOS");
        audioLibraryButton.setOnClickListener(v ->
            startActivity(new Intent(this, AudioLibraryActivity.class)));
        root.addView(audioLibraryButton, buttonParams());

        addSection(root, "4. ASISTENTE Y GAFAS");
        status = new TextView(this);
        status.setText("Guarda tus datos y activa ORVEX para habilitar el micrófono.");
        status.setTextColor(COLOR_TEXT);
        status.setTextSize(14);
        status.setPadding(dp(14), dp(14), dp(14), dp(14));
        status.setBackground(panelBackground(COLOR_PANEL, Color.rgb(26, 68, 88), 14, 1));
        root.addView(status, buttonParams());

        LinearLayout switchRow = new LinearLayout(this);
        switchRow.setOrientation(LinearLayout.HORIZONTAL);
        switchRow.setGravity(Gravity.CENTER_VERTICAL);
        switchRow.setPadding(dp(14), dp(12), dp(14), dp(12));
        switchRow.setBackground(panelBackground(COLOR_PANEL, Color.rgb(26, 68, 88), 14, 1));

        TextView switchLabel = new TextView(this);
        switchLabel.setText("ACTIVAR ORVEX");
        switchLabel.setTextColor(COLOR_TEXT);
        switchLabel.setTextSize(15);
        switchLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        switchRow.addView(switchLabel, labelParams);

        assistantSwitch = new SwitchCompat(this);
        assistantSwitch.setTextOn("ACTIVO");
        assistantSwitch.setTextOff("DESACTIVADO");
        assistantSwitch.setShowText(true);
        assistantSwitch.setChecked(prefs.getBoolean("assistant_enabled", false));
        switchRow.addView(assistantSwitch, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(switchRow, buttonParams());

        assistantSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressSwitchListener) return;
            if (isChecked) {
                activateAssistant();
            } else {
                deactivateAssistant();
            }
        });

        // Este switch es independiente del encendido general de ORVEX.
        // OFF conserva el aviso actual con confirmación; ON lee el mensaje directamente.
        LinearLayout whatsappModeRow = new LinearLayout(this);
        whatsappModeRow.setOrientation(LinearLayout.HORIZONTAL);
        whatsappModeRow.setGravity(Gravity.CENTER_VERTICAL);
        whatsappModeRow.setPadding(dp(14), dp(12), dp(14), dp(12));
        whatsappModeRow.setBackground(panelBackground(COLOR_PANEL, Color.rgb(26, 68, 88), 14, 1));

        TextView whatsappModeLabel = new TextView(this);
        whatsappModeLabel.setText("LEER MENSAJES DE WHATSAPP SIN CONFIRMACIÓN");
        whatsappModeLabel.setTextColor(COLOR_TEXT);
        whatsappModeLabel.setTextSize(14);
        whatsappModeLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams whatsappLabelParams = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        whatsappModeRow.addView(whatsappModeLabel, whatsappLabelParams);

        whatsappAutoReadSwitch = new SwitchCompat(this);
        whatsappAutoReadSwitch.setTextOn("ON");
        whatsappAutoReadSwitch.setTextOff("OFF");
        whatsappAutoReadSwitch.setShowText(true);
        whatsappAutoReadSwitch.setChecked(prefs.getBoolean("whatsapp_auto_read", false));
        whatsappModeRow.addView(whatsappAutoReadSwitch, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(whatsappModeRow, buttonParams());

        whatsappAutoReadSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("whatsapp_auto_read", isChecked).apply();
            Toast.makeText(this,
                isChecked
                    ? "WhatsApp: lectura automática sin confirmación."
                    : "WhatsApp: se solicitará confirmación antes de leer.",
                Toast.LENGTH_SHORT).show();
        });

        // Control independiente para activar o desactivar la detección de "análisis".
        LinearLayout analysisModeRow = new LinearLayout(this);
        analysisModeRow.setOrientation(LinearLayout.HORIZONTAL);
        analysisModeRow.setGravity(Gravity.CENTER_VERTICAL);
        analysisModeRow.setPadding(dp(14), dp(12), dp(14), dp(12));
        analysisModeRow.setBackground(panelBackground(COLOR_PANEL, Color.rgb(177, 111, 255), 14, 1));

        TextView analysisModeLabel = new TextView(this);
        analysisModeLabel.setText("ACTIVAR COMANDO DE ANÁLISIS");
        analysisModeLabel.setTextColor(Color.rgb(196, 151, 255));
        analysisModeLabel.setTextSize(14);
        analysisModeLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams analysisLabelParams = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        analysisModeRow.addView(analysisModeLabel, analysisLabelParams);

        analysisSwitch = new SwitchCompat(this);
        analysisSwitch.setTextOn("ON");
        analysisSwitch.setTextOff("OFF");
        analysisSwitch.setShowText(true);
        analysisSwitch.setThumbTintList(ColorStateList.valueOf(Color.rgb(190, 130, 255)));
        analysisSwitch.setTrackTintList(ColorStateList.valueOf(Color.rgb(91, 57, 126)));
        // El comando de análisis requiere validación de código antes de habilitarse.
        // En instalaciones anteriores no existe esta marca, por lo que se inicia en OFF.
        boolean analysisCodeValidated = prefs.getBoolean("analysis_code_validated", false);
        boolean analysisEnabled = analysisCodeValidated
            && prefs.getBoolean("analysis_command_enabled", false);
        analysisSwitch.setChecked(analysisEnabled);
        if (!analysisCodeValidated) {
            prefs.edit().putBoolean("analysis_command_enabled", false).apply();
        }
        analysisModeRow.addView(analysisSwitch, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(analysisModeRow, buttonParams());

        analysisSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressSwitchListener) return;
            if (isChecked) {
                // No se habilita hasta validar el código de seis dígitos con n8n.
                setAnalysisSwitchChecked(false);
                showAnalysisActivationDialog();
            } else {
                prefs.edit().putBoolean("analysis_command_enabled", false).apply();
                Toast.makeText(this,
                    "Comando análisis desactivado. Las demás órdenes siguen disponibles.",
                    Toast.LENGTH_SHORT).show();
            }
        });

        // Acceso al plan de inteligencia artificial: solo se habilita tras validar
        // un código de seis dígitos en el workflow de ORVEX.
        LinearLayout aiPlanRow = new LinearLayout(this);
        aiPlanRow.setOrientation(LinearLayout.HORIZONTAL);
        aiPlanRow.setGravity(Gravity.CENTER_VERTICAL);
        aiPlanRow.setPadding(dp(14), dp(12), dp(14), dp(12));
        aiPlanRow.setBackground(panelBackground(COLOR_PANEL, COLOR_CYAN, 14, 1));

        TextView aiPlanLabel = new TextView(this);
        aiPlanLabel.setText("PLAN DE INTELIGENCIA ARTIFICIAL");
        aiPlanLabel.setTextColor(COLOR_CYAN);
        aiPlanLabel.setTextSize(14);
        aiPlanLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams aiPlanLabelParams = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        aiPlanRow.addView(aiPlanLabel, aiPlanLabelParams);

        aiPlanSwitch = new SwitchCompat(this);
        aiPlanSwitch.setTextOn("ON");
        aiPlanSwitch.setTextOff("OFF");
        aiPlanSwitch.setShowText(true);
        aiPlanSwitch.setThumbTintList(ColorStateList.valueOf(COLOR_CYAN));
        aiPlanSwitch.setTrackTintList(ColorStateList.valueOf(Color.rgb(35, 105, 125)));
        aiPlanSwitch.setChecked(prefs.getBoolean("ai_plan_enabled", false));
        aiPlanRow.addView(aiPlanSwitch, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(aiPlanRow, buttonParams());

        aiPlanSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressSwitchListener) return;
            if (isChecked) {
                // No se habilita aquí: primero se requiere confirmación y código válido.
                setAiPlanSwitchChecked(false);
                showAiPlanActivationDialog();
            } else {
                prefs.edit().putBoolean("ai_plan_enabled", false).apply();
                Toast.makeText(this, "Plan de inteligencia artificial desactivado.", Toast.LENGTH_SHORT).show();
            }
        });

        testButton = makeButton("PROBAR RECONOCIMIENTO DE VOZ");
        testButton.setEnabled(assistantSwitch.isChecked());
        testButton.setOnClickListener(v -> {
            if (!assistantSwitch.isChecked()) {
                Toast.makeText(this, "Activa ORVEX con el switch antes de probar la voz.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!saveProfile(false)) return;
            if (!hasMicPermission()) {
                pendingPermissionAction = VoiceAssistantService.ACTION_LISTEN;
                requestMicPermission();
                return;
            }
            startVoiceService(VoiceAssistantService.ACTION_LISTEN);
            status.setText("Escuchando una consulta. Habla después del sonido o aviso del teléfono.");
        });
        root.addView(testButton, buttonParams());

        Button whatsappNotificationButton = makeButton("CONFIGURAR AVISOS DE WHATSAPP");
        whatsappNotificationButton.setOnClickListener(v -> openWhatsAppNotificationSettings());
        root.addView(whatsappNotificationButton, buttonParams());

        if (assistantSwitch.isChecked()) {
            if (hasMicPermission()) {
                startAssistantServices(false);
                status.setText("ORVEX activado. El botón central de las gafas inicia el asistente.");
            } else {
                setAssistantSwitchChecked(false);
                prefs.edit().putBoolean("assistant_enabled", false).apply();
                status.setText("Activa ORVEX y concede permiso al micrófono.");
            }
        }

        TextView instructions = new TextView(this);
        instructions.setText("GUÍA RÁPIDA\n\n01  Guarda tus datos y añade los contactos que quieras compartir con el asistente.\n02  Activa el switch ORVEX y concede permiso al micrófono.\n03  Con el switch activo, bloquea el teléfono y pulsa el botón central de las gafas.\n04  Si el switch ANÁLISIS está en ON, di ‘análisis’ entre las tres primeras palabras para activar esa acción. En OFF, esa palabra no activa el análisis. El comando ‘mensaje’ continúa funcionando.\n\nCon el plan de inteligencia artificial en ON, las consultas se envían a n8n y la respuesta se reproduce por voz. En OFF, las consultas generales no se envían. Con ORVEX activo, los mensajes de WhatsApp se anuncian por MP3. El switch adicional permite elegir lectura automática sin confirmación (ON) o lectura previa confirmación (OFF). Al desactivar el switch, el asistente deja de escuchar y los avisos de WhatsApp quedan inactivos.");
        instructions.setTextColor(COLOR_MUTED);
        instructions.setTextSize(13);
        instructions.setPadding(dp(14), dp(14), dp(14), dp(14));
        instructions.setBackground(panelBackground(Color.rgb(7, 15, 27), Color.rgb(23, 53, 73), 14, 1));
        root.addView(instructions, buttonParams());

        Button exitButton = makeButton("SALIR DE ORVEX");
        exitButton.setTextColor(Color.WHITE);
        exitButton.setBackgroundTintList(null);
        exitButton.setBackground(panelBackground(Color.rgb(49, 17, 31), COLOR_EXIT, 14, 2));
        exitButton.setElevation(dp(3));
        exitButton.setOnClickListener(v -> exitAssistant());
        root.addView(exitButton, buttonParams());

        TextView footer = new TextView(this);
        footer.setText("ORVEX  •  INTERFAZ DE CONTROL PERSONAL");
        footer.setTextColor(Color.rgb(77, 111, 133));
        footer.setTextSize(10);
        footer.setLetterSpacing(0.08f);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(18), 0, dp(4));
        root.addView(footer, matchWrap());

        setContentView(scroll);
    }

    private void openWhatsAppNotificationSettings() {
        try {
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir la configuración de avisos de Android.", Toast.LENGTH_LONG).show();
        }
    }

    private void activateAssistant() {
        if (!saveProfile(false)) {
            setAssistantSwitchChecked(false);
            prefs.edit().putBoolean("assistant_enabled", false).apply();
            if (status != null) status.setText("Completa y guarda los datos antes de activar ORVEX.");
            return;
        }
        if (!hasMicPermission()) {
            pendingPermissionAction = VoiceAssistantService.ACTION_PREPARE;
            requestMicPermission();
            return;
        }
        prefs.edit().putBoolean("assistant_enabled", true).apply();
        startAssistantServices(true);
    }

    private void startAssistantServices(boolean playActivationCue) {
        startVoiceService(VoiceAssistantService.ACTION_PREPARE);
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(playActivationCue
            ? PlaybackService.ACTION_ACTIVATE
            : PlaybackService.ACTION_ENABLE);
        startService(intent);
        if (testButton != null) testButton.setEnabled(true);
        if (status != null) {
            status.setText(playActivationCue
                ? "ORVEX activado. Después del audio inicial, pulsa el botón central de las gafas."
                : "ORVEX activado. El botón central de las gafas inicia el asistente.");
        }
    }

    private void exitAssistant() {
        // Salir siempre deja el asistente en estado INACTIVO, incluso si ya estaba apagado.
        setAssistantSwitchChecked(false);
        prefs.edit().putBoolean("assistant_enabled", false).apply();
        deactivateAssistant();
        Toast.makeText(this, "ORVEX desactivado. Hasta pronto.", Toast.LENGTH_SHORT).show();
        finishAffinity();
    }

    private void deactivateAssistant() {
        prefs.edit().putBoolean("assistant_enabled", false).apply();
        stopService(new Intent(this, VoiceAssistantService.class));
        Intent intent = new Intent(this, PlaybackService.class);
        intent.setAction(PlaybackService.ACTION_DEACTIVATE);
        try {
            startService(intent);
        } catch (Exception e) {
            stopService(new Intent(this, PlaybackService.class));
        }
        if (testButton != null) testButton.setEnabled(false);
        if (status != null) status.setText("ORVEX desactivado. El botón central no iniciará el asistente.");
    }

    private void setAssistantSwitchChecked(boolean checked) {
        if (assistantSwitch == null) return;
        suppressSwitchListener = true;
        assistantSwitch.setChecked(checked);
        suppressSwitchListener = false;
        if (testButton != null) testButton.setEnabled(checked);
    }

    private void startVoiceService(String action) {
        Intent intent = new Intent(this, VoiceAssistantService.class);
        intent.setAction(action);
        ContextCompat.startForegroundService(this, intent);
    }

    private boolean hasMicPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMicPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }
    }

    private void setAnalysisSwitchChecked(boolean checked) {
        if (analysisSwitch == null) return;
        suppressSwitchListener = true;
        analysisSwitch.setChecked(checked);
        suppressSwitchListener = false;
    }

    private void showAnalysisActivationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("ORVEX")
            .setMessage("ACTIVAR COMANDO DE ANÁLISIS?")
            .setPositiveButton("IR", (dialog, which) -> {
                Intent intent = new Intent(this, AiPlanActivity.class);
                intent.putExtra(AiPlanActivity.EXTRA_FEATURE, AiPlanActivity.FEATURE_ANALYSIS);
                startActivityForResult(intent, REQUEST_ANALYSIS_PLAN);
            })
            .setNegativeButton("VOLVER", (dialog, which) -> {
                setAnalysisSwitchChecked(false);
                prefs.edit().putBoolean("analysis_command_enabled", false).apply();
            })
            .setOnCancelListener(dialog -> {
                setAnalysisSwitchChecked(false);
                prefs.edit().putBoolean("analysis_command_enabled", false).apply();
            })
            .show();
    }

    private void setAiPlanSwitchChecked(boolean checked) {
        if (aiPlanSwitch == null) return;
        suppressSwitchListener = true;
        aiPlanSwitch.setChecked(checked);
        suppressSwitchListener = false;
    }

    private void showAiPlanActivationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("ORVEX")
            .setMessage("ACTIVAR PLAN DE INTELIGENCIA ARTIFICIAL?")
            .setPositiveButton("IR", (dialog, which) -> {
                Intent intent = new Intent(this, AiPlanActivity.class);
                intent.putExtra(AiPlanActivity.EXTRA_FEATURE, AiPlanActivity.FEATURE_PLAN);
                startActivityForResult(intent, REQUEST_AI_PLAN);
            })
            .setNegativeButton("VOLVER", (dialog, which) -> {
                setAiPlanSwitchChecked(false);
                prefs.edit().putBoolean("ai_plan_enabled", false).apply();
            })
            .setOnCancelListener(dialog -> {
                setAiPlanSwitchChecked(false);
                prefs.edit().putBoolean("ai_plan_enabled", false).apply();
            })
            .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_AI_PLAN) {
            boolean activated = resultCode == RESULT_OK
                && prefs.getBoolean("ai_plan_enabled", false);
            setAiPlanSwitchChecked(activated);
            if (activated) {
                Toast.makeText(this, "Plan de inteligencia artificial activado.", Toast.LENGTH_LONG).show();
            } else {
                prefs.edit().putBoolean("ai_plan_enabled", false).apply();
            }
        } else if (requestCode == REQUEST_ANALYSIS_PLAN) {
            boolean activated = resultCode == RESULT_OK
                && prefs.getBoolean("analysis_code_validated", false)
                && prefs.getBoolean("analysis_command_enabled", false);
            setAnalysisSwitchChecked(activated);
            if (activated) {
                Toast.makeText(this, "Comando de análisis activado.", Toast.LENGTH_LONG).show();
            } else {
                prefs.edit().putBoolean("analysis_command_enabled", false).apply();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (VoiceAssistantService.ACTION_LISTEN.equals(pendingPermissionAction)) {
                    startVoiceService(VoiceAssistantService.ACTION_LISTEN);
                    status.setText("Escuchando una consulta. Habla ahora.");
                } else {
                    activateAssistant();
                }
            } else {
                setAssistantSwitchChecked(false);
                prefs.edit().putBoolean("assistant_enabled", false).apply();
                status.setText("El permiso de micrófono es necesario para reconocer la voz.");
                Toast.makeText(this, "Permite el acceso al micrófono para usar el asistente.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean saveProfile(boolean showMessage) {
        String nombre = textOf(nombreInput);
        String cedula = textOf(cedulaInput);
        String celular = textOf(celularInput);
        String correo = textOf(correoInput);
        if (nombre.isEmpty() || cedula.isEmpty() || celular.isEmpty() || correo.isEmpty()) {
            Toast.makeText(this, "Completa nombre, cédula, celular y correo.", Toast.LENGTH_LONG).show();
            return false;
        }
        try {
            JSONObject profile = new JSONObject();
            profile.put("nombre", nombre);
            profile.put("cedula", cedula);
            profile.put("celular", celular);
            profile.put("correo", correo);
            prefs.edit().putString(KEY_PROFILE, profile.toString()).putString(KEY_CONTACTS, contacts.toString()).apply();
            if (showMessage) Toast.makeText(this, "Registro guardado en este teléfono.", Toast.LENGTH_SHORT).show();
            return true;
        } catch (JSONException e) {
            Toast.makeText(this, "No se pudieron guardar los datos.", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void addContact() {
        String name = textOf(contactoNombreInput);
        String phone = textOf(contactoTelefonoInput);
        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Escribe el nombre y teléfono del contacto.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject contact = new JSONObject();
            contact.put("nombre", name);
            contact.put("telefono", phone);
            contacts.put(contact);
            prefs.edit().putString(KEY_CONTACTS, contacts.toString()).apply();
            contactoNombreInput.setText("");
            contactoTelefonoInput.setText("");
            renderContacts();
        } catch (JSONException e) {
            Toast.makeText(this, "No se pudo agregar el contacto.", Toast.LENGTH_LONG).show();
        }
    }

    private void renderContacts() {
        if (contactsView == null) return;
        StringBuilder text = new StringBuilder("Contactos guardados: ").append(contacts.length()).append("\n");
        for (int i = 0; i < contacts.length(); i++) {
            JSONObject c = contacts.optJSONObject(i);
            if (c != null) text.append(i + 1).append(". ").append(c.optString("nombre")).append(" — ").append(c.optString("telefono")).append("\n");
        }
        text.append("\nMantén pulsada la lista para eliminar un contacto.");
        contactsView.setText(text.toString());
        contactsView.setOnLongClickListener(v -> {
            if (contacts.length() == 0) return true;
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Eliminar contacto")
                .setItems(contactLabels(), (dialog, which) -> removeContact(which))
                .setNegativeButton("Cancelar", null)
                .show();
            return true;
        });
        contactsView.setHint("Mantén pulsada esta lista para eliminar un contacto.");
    }

    private String[] contactLabels() {
        String[] labels = new String[contacts.length()];
        for (int i = 0; i < contacts.length(); i++) {
            JSONObject c = contacts.optJSONObject(i);
            labels[i] = c == null ? "Contacto" : c.optString("nombre") + " — " + c.optString("telefono");
        }
        return labels;
    }

    private void removeContact(int index) {
        JSONArray updated = new JSONArray();
        for (int i = 0; i < contacts.length(); i++) if (i != index) updated.put(contacts.opt(i));
        contacts = updated;
        prefs.edit().putString(KEY_CONTACTS, contacts.toString()).apply();
        renderContacts();
    }

    private void loadLocalData() {
        try {
            JSONObject profile = new JSONObject(prefs.getString(KEY_PROFILE, "{}"));
            // Inputs are populated after the UI is built; values are cached for that stage.
            loadedProfile = profile;
        } catch (JSONException ignored) { loadedProfile = new JSONObject(); }
        try { contacts = new JSONArray(prefs.getString(KEY_CONTACTS, "[]")); }
        catch (JSONException ignored) { contacts = new JSONArray(); }
    }

    private JSONObject loadedProfile = new JSONObject();

    private void addSection(LinearLayout root, String text) {
        TextView section = new TextView(this);
        section.setText(text);
        section.setTextColor(COLOR_CYAN);
        section.setTextSize(14);
        section.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        section.setLetterSpacing(0.06f);
        section.setPadding(dp(12), dp(13), dp(10), dp(11));
        section.setBackground(panelBackground(Color.rgb(7, 16, 28), Color.rgb(21, 66, 87), 10, 1));
        LinearLayout.LayoutParams sectionParams = matchWrap();
        sectionParams.topMargin = dp(18);
        sectionParams.bottomMargin = dp(2);
        root.addView(section, sectionParams);
    }

    private EditText addField(LinearLayout root, String label, String hint) {
        TextView caption = new TextView(this);
        caption.setText(label);
        caption.setTextColor(COLOR_MUTED);
        caption.setTextSize(12);
        caption.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        caption.setPadding(dp(3), dp(10), 0, dp(3));
        root.addView(caption, matchWrap());
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setHint(hint);
        field.setTextColor(COLOR_TEXT);
        field.setHintTextColor(Color.rgb(94, 121, 141));
        field.setTextSize(15);
        field.setSingleLine(true);
        field.setPadding(dp(12), dp(10), dp(12), dp(10));
        field.setBackground(panelBackground(Color.rgb(8, 17, 29), Color.rgb(30, 72, 92), 10, 1));
        LinearLayout.LayoutParams fieldParams = matchWrap();
        fieldParams.bottomMargin = dp(3);
        root.addView(field, fieldParams);
        if (label.equals("Nombre completo")) field.setText(loadedProfile.optString("nombre", ""));
        else if (label.equals("Cédula")) field.setText(loadedProfile.optString("cedula", ""));
        else if (label.equals("Celular")) field.setText(loadedProfile.optString("celular", ""));
        else if (label.equals("Correo electrónico")) field.setText(loadedProfile.optString("correo", ""));
        return field;
    }

    private String textOf(EditText input) { return input == null || input.getText() == null ? "" : input.getText().toString().trim(); }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.rgb(2, 16, 25));
        b.setBackgroundTintList(null);
        b.setBackground(panelBackground(COLOR_CYAN, COLOR_CYAN, 12, 1));
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextSize(13);
        b.setLetterSpacing(0.025f);
        b.setAllCaps(false);
        b.setElevation(dp(2));
        return b;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(10);
        return p;
    }

    private GradientDrawable panelBackground(int fillColor, int strokeColor, int radiusDp, int strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(strokeDp), strokeColor);
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
}
