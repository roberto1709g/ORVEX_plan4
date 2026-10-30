# ORVEX v1.6.3 — Validación del switch de análisis

## Cambios incluidos
- El switch **ACTIVAR COMANDO DE ANÁLISIS** ahora solicita confirmación y código numérico de seis dígitos antes de pasar a ON.
- La validación utiliza el mismo webhook de n8n que el plan de inteligencia artificial:
  `https://n8n.serviapps.duckdns.org/webhook/ORVEX_plan`
- El payload incluye `codigo` y `switch` para distinguir el origen:
  - `switch: "plan_ia"` para el plan de inteligencia artificial.
  - `switch: "analisis"` para el comando de análisis.
- La respuesta `true` habilita únicamente el switch solicitado; `false` mantiene ese switch en OFF.
- La pantalla de código identifica dinámicamente qué función se está activando.
- Los dos switches tienen colores distintivos: cian para Plan de IA y violeta para Análisis.
- Las instalaciones que actualicen desde versiones anteriores deberán validar una vez el switch de análisis; por seguridad, inicia en OFF si no existe validación previa.

## Archivos modificados
- `app/src/main/java/com/leaden2/app/MainActivity.java`
- `app/src/main/java/com/leaden2/app/AiPlanActivity.java`
- `app/src/main/assets/plan_ia.html`
- `app/build.gradle`

No se modificaron `VoiceAssistantService.java`, el webhook de consultas normales, los módulos de llamadas, WhatsApp ni la biblioteca de audios.
