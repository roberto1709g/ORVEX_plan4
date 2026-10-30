# ORVEX v1.5.4 — Biblioteca de audios integrada

Esta versión parte del proyecto ORVEX v1.5.3 adjunto por el usuario y agrega la biblioteca de audios probada previamente en LEADEN2.

## Cambios acotados
- Nueva pantalla nativa `AudioLibraryActivity`: seleccionar archivos de audio del teléfono, mostrar nombre original e identificador consecutivo, reproducir y eliminar de la biblioteca; botón para volver a ORVEX.
- Nuevo `AudioLibraryStore` para guardar la biblioteca localmente.
- Botón **ABRIR REPRODUCTOR DE AUDIOS** en la pantalla principal.
- La palabra **audio** se reconoce como orden local si aparece entre las tres primeras palabras reconocidas. Ejemplos: “reproducir audio uno”, “audio dos”. Estas órdenes no se envían a n8n.
- Se integra el servicio de reproducción probado, incluida la promoción a primer plano para evitar que Android lo cierre durante la pista y el control de detener con el botón central multimedia de las gafas.
- Se mantiene el flujo de confirmación de llamada de WhatsApp, la clasificación existente de llamada, mensaje, análisis y orden normal. El guard de confirmación de llamada existente conserva prioridad.
- Se registra `AudioLibraryActivity` en el manifiesto, sin quitar `WakeAndLaunchActivity` ni `WhatsAppCallAccessibilityService`.
- Versión incrementada a `1.5.4` / `versionCode 10`.

## Nota sobre la pantalla
El proyecto adjunto es una aplicación Android nativa; no usa una página HTML para su pantalla principal. Por eso la biblioteca está integrada como una pantalla nativa de Android, con el mismo estilo visual oscuro/cian del proyecto de audio ya probado. No se añadió un HTML suelto que la aplicación no fuera a utilizar.

## Verificación realizada aquí
- Manifiesto Android parseado como XML.
- Comprobado que siguen presentes los componentes de segundo plano y accesibilidad de WhatsApp.
- Comprobado que los cambios en `MainActivity` son la inserción del botón y sección de biblioteca.
- Comprobado que el código original de `VoiceAssistantService` se conserva, con el interceptor de audio añadido antes de la clasificación existente (y después de la confirmación de llamada pendiente).
- No se ejecutó `gradle assembleDebug` en este entorno porque Gradle no está instalado aquí. La compilación final debe verificarse con GitHub Actions antes de instalar el APK.
