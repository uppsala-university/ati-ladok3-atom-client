# Klient för Atom-gränssnitt
För att använda klienten som används för att hämta händelser från Ladok's ATOM-gränssnitt måste ett certifikat användas. 

Kopiera ett giltigt klientcertifikat för Ladok3 till katalogen `src/main/resources/`. Certifikatet ska vara på PKCS 12-format.

I `src/main/resources` finns en exempelfil för fordrade egenskaper. Använd den genom

`mv atomclient.properties.sample atomclient.properties`

Redigera sedan filen `src/main/resources/atomclient.properties` för att innehålla rätt namn på certifikatfil och lösenord.

Testa genom

`mvn test`
