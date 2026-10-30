# ORVEX — corrección del último paso de llamada

Esta versión parte de `ORVEX_APK_V1_5_AUDITORIA_CORRECCION_FLUJO`. Mantiene la apertura de WhatsApp y la validación del contacto.

Cambios: 
- Declara `android:canPerformGestures="true"` en el servicio de accesibilidad, requisito para que Android permita `dispatchGesture`.
- Al seleccionar la opción exacta «Llamada», intenta primero `ACTION_CLICK`; si WhatsApp no ofrece un control clicable, usa un gesto solo si el árbol de accesibilidad expone límites visibles, válidos y de tamaño razonable. No usa coordenadas fijas.
- Si no existen límites seguros, no inventa una coordenada.

**Estado:** ZIP fuente preparado y validado estructuralmente. No compilado ni probado en el teléfono. Generar el APK con GitHub Actions.
