# P2P Bank Node (Essentials Edition)

Implementace P2P projektu v jazyce Java. Aplikace funguje jako Node v Peer-to-Peer síti, kde každý uzel reprezentuje banku, spravuje vlastní účty a dokáže komunikovat s ostatními bankami pro provádění operací na vzdálených účtech.


## Klíčové vlastnosti

* **P2P Proxy:** Aplikace funguje jako Proxy server. Pokud přijme příkaz pro účet vedený u jiné banky (jiná IP adresa), automaticky naváže spojení s cílovým uzlem, předá požadavek a vrátí odpověď klientovi.
* **Fyzická Paralelizace:** Server využívá Multithreading. Pro každého připojeného klienta je vytvořeno samostatné vlákno, což umožňuje obsluhovat více klientů současně bez blokování.
* **Graceful Shutdown:** Server lze bezpečně ukončit příkazem z konzole, což zajistí správné uzavření socketů a uložení dat.
* **Databáze:** Data se ukládají do databáze 
## Požadavky a Instalace

* **Java JDK:** Verze 8 nebo novější.
* **MySQl Server**
* **Klient:** PuTTY (režim Raw).


## Spuštění Apliakce
### MySQL
* spusťte sql script v adresáři sql/schema.sql ve MySQL Workbench
* Upravte config.properties tak aby data(db.user,db.password) seděla vaším údajům
* V kořenovém adresáři projektu spusťte příkaz v terminálu:
```bash
java -jar P2PBanka.jar