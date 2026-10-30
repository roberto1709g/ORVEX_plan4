# ORVEX V1.5.1 — nota de cambio y seguridad

## Identificación
- Base funcional: `LEADEN2_APK_V1_5_CORRECCION_SELECCION_LLAMADA.zip`, reportada como exitosa por el usuario el 20-10-2026.
- Esta entrega es una nueva copia de trabajo y no reemplaza el punto de restauración original.
- Marca visible: ORVEX.
- Versión Android: `1.5.1` / `versionCode 7`.

## Regla de verificación de WhatsApp
1. ORVEX localiza primero el contacto en su agenda local y valida su número ecuatoriano.
2. Tras la confirmación hablada, abre WhatsApp mediante el número validado de ese registro.
3. En la cabecera de WhatsApp, acepta el primer nombre sin distinguir mayúsculas/minúsculas ni tildes; así `Luis` coincide con `Luis Flores`.
4. Si un texto de cabecera expone explícitamente un móvil ecuatoriano, exige que el número corresponda al número esperado.
5. El nombre no cambia el destino: la URI de WhatsApp se genera a partir del número de la agenda local. Un homónimo con otro número no es elegido por nombre.
6. Se conserva la selección del elemento `Llamada` con `ACTION_CLICK` y el fallback de gesto accesible, que fue la corrección final que permitió la llamada en la versión base.

## Límites de verificación
- Se inspeccionó estáticamente el código y se verificará la integridad del ZIP. No se dispone de un dispositivo Android ni se ha compilado este ZIP en esta sesión.
- La coincidencia por primer nombre es una verificación auxiliar de la cabecera; la identidad de destino está determinada por el teléfono guardado en ORVEX.
- Se conserva package/namespace `com.leaden2.app`, las claves de preferencias y webhook para reducir riesgo de pérdida de datos/integraciones. El cambio completo de identificador interno requeriría una migración aparte y no se realiza aquí.
