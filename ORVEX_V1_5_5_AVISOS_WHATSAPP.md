# ORVEX V1.5.5 — Avisos de mensajes de WhatsApp

Esta versión parte íntegramente de ORVEX V1.5.4 Biblioteca de Audio. Se añade el patrón validado en LEADEN2 V1.6.1 para avisos de WhatsApp, preservando la biblioteca de audios, llamadas y asistente existentes.

## Flujo
1. ORVEX activo.
2. `NotificationListenerService` detecta una notificación de WhatsApp/WhatsApp Business.
3. Se reproduce `mensaje_whatsapp.mp3`.
4. Se anuncia por TTS: `Mensaje de [persona]. ¿Deseas escucharlo?`.
5. Se escucha `sí`/`no`.
6. Con `sí`, se lee el mensaje por el parlante.
7. Con `no` o respuesta no reconocida, se cancela.

Incluye el botón `CONFIGURAR AVISOS DE WHATSAPP`. El aviso se mantiene inactivo cuando ORVEX está desactivado.

El audio de biblioteca existente se conserva; si está reproduciéndose, el aviso de WhatsApp queda pendiente para no interrumpirlo y se procesa al terminar.
