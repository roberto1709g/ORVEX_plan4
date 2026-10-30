# ORVEX V1.5.3 — confirmación de llamada breve

Esta versión parte de ORVEX V1.5.1, cuyo primer ajuste fue probado con éxito por el usuario: la agenda local puede guardar `Luis` y WhatsApp mostrar `Luis Flores`, siempre que el destino sea el número correcto. También conserva la corrección de accesibilidad que permitió iniciar una llamada de voz real.

## Segundo ajuste: frases de voz más cortas

- Antes de la confirmación, ORVEX dice: **«He encontrado a [nombre], ¿deseas continuar?»**
- Si la persona responde **«sí»**, ORVEX dice únicamente: **«Confirmado.»** y, al terminar esa frase, continúa el flujo de apertura de WhatsApp y llamada automática.
- Si responde **«no»**, cancela la operación y no abre WhatsApp para esa orden.

La lógica de escucha tras la pregunta se ha ajustado para que no dependa de la antigua frase «Responde sí para confirmar o no para cancelar». La continuación hacia WhatsApp se ha ajustado para depender de la frase exacta «Confirmado.» mientras exista un número pendiente validado. Esto es esencial: cambiar solamente el texto habría podido impedir que se abriera WhatsApp al finalizar la locución.

## Versionado

- Nombre visible: ORVEX.
- Versión Android: `1.5.2` / `versionCode 8`.
- Identificador interno conservado: `com.leaden2.app`.
- Se conservan las preferencias internas, la validación por número, la coincidencia por primer nombre y la automatización de llamada que ya funcionó.

## Compilación y prueba dirigida

1. Extrae el ZIP y sube el contenido del directorio del proyecto a una rama de trabajo de GitHub.
2. Ejecuta el workflow **Build APK ORVEX V1.5.3**.
3. Descarga el artefacto `orvex-app-v1-5-2-whatsapp-auto-call`.
4. Prueba una vez con un contacto de confianza: debe decir la pregunta breve, aceptar «sí», decir solo «Confirmado.» y continuar a la llamada.
5. Prueba la cancelación «no» por separado: debe cancelar y no iniciar la llamada.

## Estado y límites

Este ZIP contiene código fuente. No se afirma que esté compilado ni probado en el teléfono en el momento de su entrega. La llamada automática y la coincidencia por primer nombre funcionaron en versiones anteriores según la prueba reportada por el usuario; este ajuste de locución necesita su propia validación.

No modifica LEADEN1/V1.4, n8n, Docker, Nginx, HTTPS ni los workflows del VPS. Conserva el paquete Android para reducir riesgo de perder datos; una actualización instalada requiere que la firma coincida con la APK existente.
