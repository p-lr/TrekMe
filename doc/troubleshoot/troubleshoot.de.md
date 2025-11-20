# Was sollte ich tun, wenn...

* [Ich meine Position auf der Karte nicht erhalte](#ich-meine-position-auf-der-karte-nicht-erhalte)
* [Beim Erstellen einer neuen Karte eine Meldung erscheint, die mich auffordert, meine Internetverbindung zu überprüfen](#beim-erstellen-einer-neuen-karte-eine-meldung-erscheint-die-mich-auffordert-meine-internetverbindung-zu-überprüfen)
* [Meine GPX-Aufzeichnung manchmal von selbst stoppt](#meine-gpx-aufzeichnung-manchmal-von-selbst-stoppt)
* [Gerade Linien in meiner GPX-Aufzeichnung erscheinen](#gerade-linien-in-meiner-gpx-aufzeichnung-erscheinen)
* [Ich ein anderes Problem habe](#ich-habe-ein-anderes-problem)


## Ich meine Position auf der Karte nicht erhalte

Es gibt mehrere mögliche Ursachen für dieses Problem. Bitte überprüfen Sie Folgendes:

1. Befinden Sie sich an einem Standort, an dem das GPS-Signal empfangen werden kann?\
   Geschlossene Umgebungen wie Häuser, Wohnungen usw. erschweren den Empfang des GPS-Signals (denken Sie daran, es kommt von Satelliten).

2. Haben Sie TrekMe den Zugriff auf Ihren Standort autorisiert?\
   Sie können dies in Ihren Android-Einstellungen für die TrekMe-App überprüfen. Die Berechtigungen sind aufgeführt und „Standort“ sollte aktiviert sein.

3. Ist der Standort auf Ihrem Gerät aktiviert?\
   Obwohl TrekMe alle notwendigen Berechtigungen erteilt wurden, kann der Standort auf dem Gerät deaktiviert sein, was verhindert, dass alle Apps (einschließlich TrekMe) auf Ihren Standort zugreifen.

## Beim Erstellen einer neuen Karte eine Meldung erscheint, die mich auffordert, meine Internetverbindung zu überprüfen

Obwohl TrekMe für Wanderungen im Offline-Modus gedacht ist, benötigen Sie beim Erstellen einer neuen Karte einen Internetzugang. Wenn Sie bestätigen können, dass Ihre Internetverbindung einwandfrei funktioniert, liegt möglicherweise ein Problem mit den Kartenanbietern vor – das kommt manchmal vor. Im letzteren Fall wird empfohlen, einige Minuten zu warten, bevor Sie einen weiteren Versuch unternehmen.

## Meine GPX-Aufzeichnung manchmal von selbst stoppt

Eine GPX-Aufzeichnung ist eigentlich ein Android-Dienst, der im Hintergrund ausgeführt wird. Einige Geräte legen jedoch eine harte Grenze für die Anzahl der gleichzeitig ausgeführten Hintergrunddienste fest.
Es ist nicht ungewöhnlich, dass Android einen Dienst abrupt stoppt, wenn es der Ansicht ist, dass das Anhalten die Akkulaufzeit verlängern würde. Im Falle einer GPX-Aufzeichnung ist es jedoch vertretbar, etwas Strom zu verbrauchen, solange Sie sich dessen bewusst sind.

Um dieses Problem zu beheben, müssen Sie zu den Geräteeinstellungen -> „Akku“ gehen. Dort sollten Sie ein Menü „Akku-Optimierung“ oder Ähnliches sehen. Es sollte eine Liste von Anwendungen angezeigt werden. Scrollen Sie nach unten, um TrekMe zu finden, und deaktivieren Sie die Akku-Optimierung.

## Gerade Linien in meiner GPX-Aufzeichnung erscheinen

Dieses Problem kann zwei Hauptursachen haben:

1. Die Akku-Optimierung ist für TrekMe aktiv. Befolgen Sie die Anweisungen unter [Meine GPX-Aufzeichnung manchmal von selbst stoppt](#meine-gpx-aufzeichnung-manchmal-von-selbst-stoppt).

2. Die Standort-Berechtigung ist nicht auf „Immer zulassen“ eingestellt. Die Erlaubnis, den Standort nur bei Nutzung der App zu verwenden, ist nicht ausreichend.

## Ich habe ein anderes Problem

Wenn Ihr Problem oben nicht aufgeführt ist, kontaktieren Sie mich unter plr.devs@gmail.com

Ich werde mein Bestes tun, um Ihnen zu helfen.