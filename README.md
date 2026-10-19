# Bewässerungs- & Gewächshaussteuerung

Genauere Beschreibungen siehe:
dokument/PlanOfArduinos.odt
dokument/ArduinoCodeHints.odt


Es geht um die Bewässerung eines 3 Rai großen Geländes auf dem derzeit 11 Reihen mit bis zu 8 Plätzen pro Reihe vorhanden sind.
Es wird ein Raspberry für Datenbank und Webserver eingeplant.

An den Raspberry wird eine Wetterstation angebunden
z.B.: 
Pimoroni Weather HAT + Weather Sensor Kit
Ecowitt WittBoy oder HP2551 / HP2560 Serien

Erfassung von Wind, Regen und Lichtstärke (Helligkeit)


Für jeden Platz sollen folgende Parameter erfasst werden:

1. Feuchtemessung in 10 cm Tiefe
2. Feuchtemessung in 35 cm Tiefe

3. Gesteuert wird ein 1/2" motorisierten 2-Wege-Kugelhahn (12V oder 24V) zum Öffnen und Schließen
4. 1/2" Durchflussmesser - Modbus-RTU Impulszähler-Modul (Pulse Counter) (z. B. Module wie tMPC-04 oder industrielle DIN-Schienen-Zähler).

Zu prüfen ist wie viele Plätze von einem Arduino verwaltet werden können.


Das Modbus-Adressen-Schema pro Reihe

Da jede Reihe autark arbeitet, hat jede Reihe ihren eigenen RS485-Bus / Modbus, der am Reihen-Arduino endet. Sie können für jede Reihe exakt dieselbe Adressstruktur verwenden:

Gerät an der Reihe		Modbus Slave ID	Funktion
Sensor 10 cm Tiefe		ID 1	Liefert Feuchtigkeit der oberen Bodenschicht
Sensor 35 cm Tiefe		ID 2	Liefert Feuchtigkeit der Hauptwurzel-Zone
Impulszähler (Durchfluss)	ID 3	Speichert die durchgelaufenen Liter Wasser
Relais-Modul (Ventil)		ID 4	Öffnet / schließt die 24V-Zuleitung zum Kugelhahn

