# ORVEX v1.6.2 — Plan de Inteligencia Artificial

## Cambios
- Nuevo switch independiente: PLAN DE INTELIGENCIA ARTIFICIAL.
- El switch inicia en OFF si todavía no existe una activación validada.
- Al intentar activarlo, muestra el diálogo "ACTIVAR PLAN DE INTELIGENCIA ARTIFICIAL?" con IR y VOLVER.
- IR abre `app/src/main/assets/plan_ia.html`, con campo numérico de seis dígitos y botón VOLVER.
- La pantalla envía un POST HTTPS a `https://n8n.serviapps.duckdns.org/webhook/ORVEX_plan`.
- Cuerpo JSON enviado: `{"codigo":"123456"}`.
- Solo una respuesta `true` (también se acepta JSON boolean true o un objeto con valid/success/resultado/authorized=true) activa el plan.
- Una respuesta false o un error mantiene el switch en OFF y muestra un mensaje.
- `VoiceAssistantService.sendToWebhook()` verifica `ai_plan_enabled` antes de enviar cualquier consulta de voz a n8n. Si está OFF, no realiza la petición y comunica que el plan está desactivado.
- Cuando está ON, conserva el envío actual y la reproducción por voz de la respuesta del agente, que ya existía en la versión base.
- El switch ANÁLISIS permanece independiente.

## Archivos nuevos
- `app/src/main/java/com/leaden2/app/AiPlanActivity.java`
- `app/src/main/assets/plan_ia.html`

## Archivos modificados
- `app/src/main/java/com/leaden2/app/MainActivity.java`
- `app/src/main/java/com/leaden2/app/VoiceAssistantService.java`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle`

## Integración esperada en n8n
El webhook `ORVEX_plan` debe aceptar POST JSON y leer el código desde `body.codigo` (en el nodo Webhook de n8n). La respuesta debe ser el booleano JSON `true` si el código es válido o `false` si no lo es.

## Verificación
Se verificó el XML del manifiesto y la estructura del paquete. No se ejecutó compilación Gradle ni pruebas en dispositivo en este entorno.
