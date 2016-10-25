# Klient för Atom-gränssnitt
För att använda klienten och kunna hämta händelser från Ladoks ATOM-gränssnitt måste ett certifikat användas. 

Kopiera ett giltigt klientcertifikat för Ladok3 till katalogen `src/main/resources/`. Certifikatet ska vara på PKCS 12-format.

I `src/main/resources` finns en exempelfil för fordrade egenskaper. Använd den genom

    cp atomclient.properties.sample atomclient.properties

Redigera sedan filen `src/main/resources/atomclient.properties` så att den innehåller rätt namn på certifikatfil och lösenord.

Därefter kan du köra integrationstesterna med detta kommando:

    mvn clean verify -Prun-its
