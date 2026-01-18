# P2P Bank Node (Essentials Edition)

Implementace P2P projektu v jazyce Java. Aplikace funguje jako Node v Peer-to-Peer síti, kde každý uzel reprezentuje banku, spravuje vlastní účty a dokáže komunikovat s ostatními bankami pro provádění operací na vzdálených účtech.


## Klíčové vlastnosti

* **P2P Proxy:** Aplikace funguje jako Proxy server. Pokud přijme příkaz pro účet vedený u jiné banky (jiná IP adresa), automaticky naváže spojení s cílovým uzlem, předá požadavek a vrátí odpověď klientovi.
* **Fyzická Paralelizace:** Server využívá Multithreading. Pro každého připojeného klienta je vytvořeno samostatné vlákno, což umožňuje obsluhovat více klientů současně bez blokování.
* **Graceful Shutdown:** Server lze bezpečně ukončit příkazem z konzole, což zajistí správné uzavření socketů a uložení dat.

## Požadavky a Instalace

* **Java JDK:** Verze 8 nebo novější.
* **Klient:** PuTTY (režim Raw).

### Kompilace
V kořenovém adresáři projektu spusťte příkaz v terminálu:
```bash
java -jar P2PBanka.jar