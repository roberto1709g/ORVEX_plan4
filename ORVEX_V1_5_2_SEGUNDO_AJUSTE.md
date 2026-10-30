# ORVEX V1.5.2 — registro del segundo ajuste

## Objetivo
Acortar la locución de confirmación sin romper el encadenamiento entre Text-to-Speech y la apertura de WhatsApp.

## Textos acordados
- Pregunta: `He encontrado a [nombre], ¿deseas continuar?`
- Respuesta afirmativa: `Confirmado.`
- Respuesta negativa: se mantiene la cancelación existente.

## Código ajustado
`app/src/main/java/com/leaden2/app/VoiceAssistantService.java`

1. `processLocalCallRequest()` anuncia la pregunta breve.
2. `processCallConfirmation()` acepta la respuesta afirmativa y, tanto con accesibilidad activada como desactivada, pronuncia solo `Confirmado.`. Si la accesibilidad está desactivada, se abre el chat según el flujo existente, pero la llamada automática depende de tener el servicio habilitado.
3. `speakPending()` asigna `leaden2-call-confirmation` cuando la locución comienza con `He encontrado a ` y aún se espera la confirmación; esto permite volver a escuchar el sí/no.
4. `speakPending()` asigna `leaden2-whatsapp-open` cuando hay un número pendiente validado y la frase es `Confirmado.`; al terminar la locución, el callback abre WhatsApp. Este ajuste evita que la reducción del texto rompa el flujo automático.

## Versionado y conservación
- `versionName`: `1.5.2`; `versionCode`: `8`.
- Se conserva `applicationId`/namespace `com.leaden2.app`, preferencias, webhook, coincidencia por primer nombre y selección accesible de «Llamada».
- No se toca LEADEN1/V1.4 ni servicios del VPS.

## Validación
La estructura del ZIP y las modificaciones textuales se verifican antes de entrega. No se dispone de compilación Android ni prueba de dispositivo en esta sesión; confirmar en el teléfono ambos caminos: `sí` inicia la llamada y `no` cancela.
