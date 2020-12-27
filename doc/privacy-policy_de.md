# Datenschutzerklärung

Diese Datenschutzerklärung beschreibt welche Daten in der App *Social Distance Meter* erfasst, und wie sie verarbeitet werden.

Die Information der Datenschutzerklärung beziehen sich auf Version **0.1.2** der App, **Stand 27.12.2020**. 

Sofern es keine grundlegenden Änderungen an der App gab, ist diese Datenschutzerklärung auch für neuere Versionen der App gültig. Sollten Änderungen an der App nicht mehr zu den Informationen dieser Datenschutzerklärung passen, wird diese an den aktuellen Stand der App angepasst.

## Welche Daten werden durch die App erfasst?
Die Hauptfunktion der App ist das Erfassen von ausgesendeten Bluetooth Low Energy (BLE) Token des Exposure Notification Frameworks (ENF) durch andere Geräte in der Umgebung.
Diese Token werden mit Zeitstempel der Erfassung, der Empfangsqualität (RSSI) und der Absender-MAC-Adresse gespeichert. Die Erfassung erfolgt jede Minute, kann jedoch durch den Benutzer auch auf größere Zeiträume eingestellt werden.

Der Benutzer kann eine Erfassung des Standorts in den Einstellungen aktivieren (Opt-In). Der Standort wird nach Aktivierung, je nach Einstellung, alle 5 bis 15 Minuten festgestellt, den erfassten Token zugeordnet und gespeichert.

## Welche Daten werden durch externe Dienste erfasst?
Die App synchronisiert Diagnoseschlüssel von offiziellen nationalen Warn-Apps. Dabei werden die Diagnoseschlüssel der offiziellen Apps via HTTPS abgerufen.

Beim Abruf der Diagnoseschlüssel können Daten durch den jeweiligen Betreiber erfasst werden. Details hierzu finden sich in der Datenschutzerklärung des jeweiligen Anbieters:
* Deutschland: https://www.coronawarn.app/assets/documents/cwa-privacy-notice-de.pdf

## Wo werden die Daten gespeichert?
Alle durch die App erfassten Daten werden in einer Datenbank im geschützten App-Speicher auf dem jeweiligen Gerät gespeichert.

## Wie lange werden Daten gespeichert?
Die erfassten Daten bleiben in der Regel bis zu 15 Tage auf dem Gerät gespeichert.

Einmal täglich findet eine Bereinigung der erfassten Daten statt. Diese löscht alle erfassten Daten die älter als 14 Tage sind.

## Werden Daten übertragen?
Die App überträgt keine Daten. Alle erfassten Daten bleiben ausschließlich auf dem Gerät gespeichert.

## Welche Berechtigungen benötigt die App und warum?
* *Exakter Standort*: 
Diese Berechtigung wird benötigt um Bluetooth Low Energy (BLE) Token erfassen zu können. Google setzt diese Berechtigung voraus, da einige BLE-Token (nicht die des Exposure Notification Frameworks (ENF)) Aufschluss über den genauen Standort des Gerätes liefern können. Ein Erfassen von BLE-Token ohne diese Berechtigung ist nicht möglich.
Eine Standorterfassung innerhalb der App findet nur mit aktivierter Einstellung zur Standorterfassung statt.

* *Standort im Hintergrund*:
Da die App auch dann BLE-Token erfassen soll wenn diese gerade nicht im Vordergrund aktiv ist, wird die Berechtigung zum Zugriff auf den Standort im Hintergrund benötigt. Wird die Berechtigung nicht erteilt kann die App nur dann BLE-Token erfassen wenn sie aktiv im Vordergrund läuft.

* *Vordergrunddienst*:
Das Erfassen der BLE-Token erfolgt in regelmäßigen, wiederkehrenden Zyklen. Um sicherstellen zu können, dass diese regelmäßigen Zyklen möglich sind, und der Dienst zur Erfassung im Hintergrund nicht durch das Betriebssystem beendet wird, ist diese Berechtigung notwendig. Nur so kann sichergestellt werden, dass der Dienst langfristig aktiv im Hintergrund laufen darf.

* *Bluetooth*:
Ohne Berechtigung zum Zugriff auf Bluetooth können auch keine BLE-Token erfasst werden.

* *Beim Start ausführen*:
Damit der Dienst zum Erfassen der BLE-Token auch nach einem Neustart des Geräts weiter läuft ist diese Berechtigung notwendig. Ansonsten müsste die App nach einem Neustart des Geräts händisch gestartet werden. 

* *Netzwerk*:
Zum Abgleich der Diagnoseschlüssel benötigt die App Zugriff auf das Internet.

## Warum wird die Standortberechtigung benötigt, obwohl ich diesen gar nicht erfassen möchte?
Google setzt die Berechtigung zum Zugriff auf den exakten Standort voraus, da einige BLE-Token (nicht die des Exposure Notification Frameworks (ENF)) Aufschluss über den genauen Standort des Gerätes liefern können. Ein Erfassen von BLE-Token ohne diese Berechtigung ist nicht möglich.
Eine Standorterfassung innerhalb der App findet nur mit aktivierter Einstellung zur Standorterfassung statt.

# Kontakt
Für Fragen und Anliegen rund um die Datenschutzerklärung der App, ist der Author über https://github.com/BaaaZen/social-distance-meter oder über baaazen@gmail.com erreichbar.
