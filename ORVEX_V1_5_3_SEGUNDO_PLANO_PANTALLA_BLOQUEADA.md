# ORVEX V1.5.3 — Apertura desde segundo plano y pantalla bloqueada

## Objetivo
Conservar el flujo de voz confirmado en V1.5.2 y mejorar la apertura de WhatsApp cuando ORVEX está en segundo plano o la pantalla está bloqueada.

## Cambios
- Nueva actividad de transición `WakeAndLaunchActivity`.
- Solicitud de encendido de pantalla y presentación sobre la pantalla de bloqueo.
- Si Android detecta que el teléfono está bloqueado, se solicita el flujo de desbloqueo normal; ORVEX no intenta eludir PIN, patrón ni biometría.
- Después de que Android indique que el teléfono está desbloqueado, se intenta abrir el chat del número validado con `whatsapp://send`, y `https://wa.me/` como alternativa.
- En Android 14 o superior se envía la solicitud mediante `PendingIntent` con el modo de inicio de actividad en segundo plano permitido explícitamente.
- Versionado actualizado a `versionCode 9`, `versionName 1.5.3`.

## No se modificó
- Reconocimiento y confirmación de voz.
- Agenda local y normalización del teléfono.
- Contrato del webhook.
- Servicio de accesibilidad que identifica el contacto y selecciona la opción exacta de llamada de voz.
- Identificador de paquete `com.leaden2.app`.

## Validación pendiente
Este ZIP contiene código fuente; no se ha compilado ni probado en el teléfono. Compilar con GitHub Actions y validar por separado:
1. ORVEX visible.
2. Pantalla de inicio con ORVEX en segundo plano.
3. Pantalla bloqueada con desbloqueo manual.
4. Respuesta «no», para comprobar que no se abre WhatsApp.
