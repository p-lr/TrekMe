# ¿Qué debo hacer si...

* [No puedo obtener mi posición en el mapa](#no-puedo-obtener-mi-posición-en-el-mapa)
* [Al crear un nuevo mapa, un mensaje me pide que revise mi conexión a internet](#al-crear-un-nuevo-mapa-un-mensaje-me-pide-que-revise-mi-conexión-a-internet)
* [Mi grabación GPX a veces se detiene sola](#mi-grabación-gpx-a-veces-se-detiene-sola)
* [Aparecen líneas rectas en mi grabación GPX](#aparecen-líneas-rectas-en-mi-grabación-gpx)
* [Tengo otro problema](#tengo-otro-problema)


## No puedo obtener mi posición en el mapa

Hay varias causas posibles para este problema. Siga estas comprobaciones:

1. ¿Se encuentra en una ubicación donde se puede recibir la señal GPS?
   Los entornos cerrados como casas, apartamentos, etc., dificultan la recepción de la señal GPS (recuerde,
   proviene de satélites).

2. ¿Ha autorizado a TrekMe a acceder a su ubicación?
   Puede verificar esto en la configuración de Android, para la aplicación TrekMe. Los permisos están listados y
   "Ubicación" debe estar marcado.

3. ¿Está habilitada la ubicación en su dispositivo?
   Aunque se hayan otorgado todas las autorizaciones necesarias a TrekMe, la ubicación puede estar deshabilitada en el dispositivo,
   lo que impide que todas las aplicaciones (incluida TrekMe) accedan a su ubicación.

## Al crear un nuevo mapa, un mensaje me pide que revise mi conexión a internet

Si bien TrekMe está pensado para el senderismo sin conexión, necesita tener acceso a internet al
crear un nuevo mapa. Si puede confirmar que su conexión a internet funciona bien, entonces podría ser un
problema con los proveedores de mapas; a veces sucede. En este último caso, se recomienda esperar unos
minutos antes de hacer otro intento.

## Mi grabación GPX a veces se detiene sola

Una grabación GPX es en realidad un servicio de Android que se ejecuta en segundo plano. Sin embargo, algunos dispositivos
establecen un límite estricto en el número de servicios en segundo plano permitidos.
No es raro que Android detenga abruptamente un servicio, si considera que detenerlo
prolongaría la duración de la batería. En el caso de una grabación GPX, es razonable consumir un poco de energía mientras
usted es consciente de ello.

Para solucionar este problema, debe ir a la configuración de su dispositivo -> "Batería". Luego, debería ver un menú
"Optimización de batería" o similar. Debería ver una lista de aplicaciones. Desplácese hacia abajo para encontrar TrekMe
y deshabilite la optimización de la batería.

## Aparecen líneas rectas en mi grabación GPX

Este problema puede tener dos causas fundamentales:

1. La optimización de la batería está activa para TrekMe. Siga las instrucciones para [Mi grabación GPX a veces se detiene sola](#mi-grabación-gpx-a-veces-se-detiene-sola).

2. El permiso de ubicación no está configurado en "Permitir todo el tiempo". Permitir la ubicación solo cuando se usa
   la aplicación no es suficiente.

## Tengo otro problema

Si su problema no está listado arriba, contácteme en plr.devs@gmail.com

Haré todo lo posible para ayudarle.