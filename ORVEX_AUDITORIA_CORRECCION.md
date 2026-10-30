# ORVEX V1.5 — Corrección del flujo de llamada (candidata)

Esta copia parte de la versión de llamada automática con búsqueda en ventanas, no de las versiones de diagnóstico que desarmaban la orden al fallar la verificación.

Cambios:
- Se intenta abrir el chat con `whatsapp://send?phone=...` antes de usar `https://wa.me/...`, para evitar la pantalla intermedia `Enviar a... / Buscando...` observada en el teléfono.
- La verificación de teléfono admite formatos ecuatorianos local `09...` e internacional `5939...`.
- El tiempo máximo de la operación aumenta a 90 segundos para cubrir la carga del chat y el menú.
- Los mensajes de estado del servicio se registran en Logcat y no aparecen como Toasts encima de WhatsApp.
- Se conserva el requisito de verificar el contacto y de identificar la opción exacta de llamada de voz antes de pulsar. No se utilizan coordenadas fijas ni bucles de gestos.

Importante: proyecto fuente candidato; no está compilado ni probado en el dispositivo. La URI directa puede comportarse de manera distinta según la versión instalada de WhatsApp. No se afirma que la llamada automática esté resuelta hasta probarla.
