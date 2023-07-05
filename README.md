# Onkostar Plugin "onkostar-plugin-mafrepo"

Plugin zum Abrufen von Informationen aus der Anwendung "maf-repo" und zur Übernahme in das Onkostar-Formular "OS.Molekulargenetik"

Die Informationen werden vom Remote-System abgerufen und so aufbereitet, dass sie im Formular automatisch zur Verfügung stehen.
Dies umfasst auch die Merkmalskatalogversion, die beim Setzen von Werten von Merkmalskatalogen erforderlich ist.

# Beispielscript

Das Script in [examples/script.js](examples/script.js) zeigt die Nutzung des Plugins nach Drücken eines Buttons.

Zusätzlich wird die erweiterte Dokumentation erzwungen und die Analyse-Methode "Sequenzierung" aktiviert, was zur Erfassung von einfachen Varianten erforderlich ist.

# Einstellungen

Zum Betrieb dieses Plugins ist die Angabe der URL der MAF-Repo-Anwendung erforderlich.

Dies lässt sich initial durch folgende Datenbankanfrage anlegen, später dann in den allgemeinen Einstellungen von Onkostar auch ändern.

```
INSERT INTO einstellung (name, wert, kategorie, beschreibung) VALUES('mafrepo_url', 'http://localhost:8000', 'System', 'MAF-Repo - URL');
```