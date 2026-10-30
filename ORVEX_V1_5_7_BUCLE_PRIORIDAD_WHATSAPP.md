# ORVEX v1.5.7 — Bucle de audio y prioridad de WhatsApp

Cambios quirúrgicos sobre la versión ORVEX v1.5.6:

- Se añade el comando local `bucle [número]`, reconocido cuando la palabra `bucle` aparece entre las tres primeras palabras reconocidas.
- `bucle cuatro` reproduce continuamente la pista 4 mediante repetición de una sola pista.
- El comando existente `audio [número]` conserva la reproducción de una sola vez.
- Ambos comandos se procesan localmente y no se envían al webhook de n8n.
- El botón central de las gafas detiene tanto una pista normal como una pista en bucle y restablece el modo de repetición.
- Si llega una notificación de mensaje de WhatsApp durante una pista de biblioteca normal o en bucle, la pista se interrumpe y se reproduce el aviso `mensaje_whatsapp.mp3`; después se mantiene el flujo existente de confirmación del mensaje.
- No se modifican los clasificadores ni los flujos de llamada, mensaje, análisis u orden directa a n8n.

Validación realizada sobre el código fuente:
- Verificación de estructura y llaves de los dos archivos Java modificados.
- Verificación de que el manifiesto Android sigue siendo XML válido.
- Verificación de las referencias de las acciones y del número de versión.
- Integridad del ZIP generado.

Pendiente: compilar con GitHub Actions e instalar en el dispositivo para validar el comportamiento real de voz, repetición y prioridad de WhatsApp.
