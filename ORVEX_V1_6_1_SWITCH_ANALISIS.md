# ORVEX v1.6.1 — Switch independiente para el comando análisis

## Cambio
Se añadió a la pantalla principal el switch **ACTIVAR COMANDO DE ANÁLISIS**.

- ON: reconoce `análisis` entre las tres primeras palabras y conserva el envío a n8n con el campo `analisis`.
- OFF: no clasifica `análisis` como acción especial. El texto continúa por el flujo normal y las demás acciones (llamada, mensaje, audio/bucle y orden general) mantienen su lógica.
- Preferencia persistente: `analysis_command_enabled`.
- Valor predeterminado: ON, para conservar el comportamiento anterior al instalar esta actualización.

## Archivos modificados
- `MainActivity.java`: interfaz, persistencia y aviso del switch.
- `VoiceAssistantService.java`: la detección de análisis respeta el estado del switch.
- `app/build.gradle`: versión 1.6.1, versionCode 17.

## Pruebas pendientes
El ZIP se ha revisado estructuralmente y el manifiesto XML es válido. Debe compilarse mediante GitHub Actions y probarse en el dispositivo antes de considerarlo validado.
