# Byggestatus

![Byggestatus](https://travis-ci.org/kartverket/eksempelklient-etinglysing-altinn.svg?branch=release "Byggestatus")

# Formål
Eksempelklient som illustrerer enkelte scenarier av elektronisk tinglysing for eksterne aktører. Formålet med klienten er å demonstrere hvordan formidlingstjenesten i Altinn blir benyttet for å sende inn meldinger til elektronisk tinglysing i Kartverket.


# Forutsetninger
JDK installert og konfigurert (JAVA_HOME og PATH). Testet med JDK versjon 8. Git benyttes for utsjekk av kode.
Gradle benyttes som byggesystem.

Tilgang til å kalle systemet fås ved henvendelse til tinglysingavdelingen. Man må bli lagt inn som bruker hos tinglysingen.
I tillegg må man ha en systembruker i Altinn med rettighet til å kalle formidlingstjenesten. For oppsett av systembruker i Altinn, se avsnitt lengre ned.

# Bruk
Klone git repository til lokalt repo.

Byggesystemet kalles via "gradlew" kommando.
Eksempelvis vil "gradlew tasks" liste ut alle dokumenterte tasker i byggesystemet.
Se gruppen 'Eksempelklient tasks' for aktuelle tasker.

For kompilering og kjøring av eksempelklient der man sender inn en forsendelse til tinglysing:

`$ gradlew assemble demonstrerSendTilTinglysing -Daltinn.server=https://tt02.altinn.basefarm.net -Daltinn.serviceCode=4433 -Daltinn.serviceEditionCode=xx -Daltinn.user=xxxx -Daltinn.password=xxxx -Daltinn.reportee=xxxxxxxx -Daltinn.recepient=910976168`

Beskrivelse av input parametere:

* altinn.server = angir server adresse for Altinn formidlingstjenesten, for test benyttes https://tt02.altinn.basefarm.net
* altinn.serviceCode/altinn.serviceEditionCode angir utgave av Kartverket sin tjeneste
* altinn.user/altinn.password må settes opp av innsender selv i Altinn sluttbruker løsning, se avsnitt lengre ned
* altinn.reportee - organisasjonsnummer for innsender, for test får man tildelt et fiktivt organisasjonsnummer
* altinn.recepient - organisasjonsnummer for Kartverket. Test: 910976168. Produksjon: 971040238

Alternativt til angivelse av system properties på kommandolinjen (via -D) så kan disse defineres i property-fil.
Se filen 'gradle.properties' for setting av parametere.

# IDE
Prosjektet kan f.eks. lastes inn i Jetbrains IntelliJ IDEA ved å importere gradle prosjektet.
IntelliJ finnes som gratisversjon (Community Edition) og kan lastes ned via https://www.jetbrains.com/idea/download/
Andre IDEs kan også benyttes, f.eks. Eclipse, men dette har ikke blitt testet.

# Testscenarier i eksempelklienten
Eksempelet inneholder innsending av tre filer til tinglysing. Først en pant, deretter en duplikat melding med samme innhold og tilslutt innsending av en ikke gyldig xml. Legg merke til at for at eksemplene skal kjøre hensiktsmessig må matrikkelenheter og personer finnes i det miljøet man sender inn filer til.
Den første filen vil bli sendt inn og godkjent, den andre vil bli avvist i mottak pga duplikat foresendelsereferanse og den siste vil bli avvist pga feil i format. Alle filer blir først sendt inn, deretter venter man på at kvittering skal bli tilgjengelig for alle filene.
Tilslutt venter man på statusoppdateringer for filene som har blitt sendt inn helt til eksempelklienten stenges. Legg merke til at for test blir det benyttet fiktive data i for Altinn spesifikke ting, mens innholdet som skal mottas av Kartverket benytter reelle data. 

I katalogen 'src/main/resources/eksempelfiler' finnes også en rekke eksempler på andre type forsendelser.

# Eksterne grensesnitt
Eksempelet inneholder gjeldende versjon av WSDL-er og skjema-filer for Altinn formidlings tjenester. I tillegg er det en avhengighet til en artifakt som inneholder Kartverkets WSDL-er og skjema-filer for InnsendingsService. 
Denne artefakten er hostet i kartverket sitt eksterne nexus repository. Dette er definert i byggefilen. Tilsvarende må legges inn 
i andre byggefiler hvis prosjeket skal bygge feks med maven

    repositories {
        mavenCentral()
        maven {
           url "https://nexus.grunnbok.no/repository/maven-public/"
        }
    }

Dokumentasjon av Altinn sine tjenester finnes her:
https://altinnett.brreg.no/no/Sluttbrukersystemer/

## Grensesnitt mot Altinn sin formidlingstjeneste
Fra Altinn sine tjenester benyttes følgende:

* Initiering av innsending: BrokerService.initiateBrokerServiceBasic
* Opplasting av fil: BrokerServiceStreamed.uploadFileStreamedBasic
* Hent kvitteringsstatus for opplastet fil: ReceiptExternalBasic.getReceiptBasic
* Sjekk om det finnes filer klare for nedlasting: BrokerServiceExternalBasic.getAvailableFilesBasic
* Last ned filer: BrokerServiceExternalBasicStreamed.downloadFileStreamedBasic
* Bekreft nedlasting av fil:  BrokerService.confirmDownloadBasic

### Innsending av forsendelse

Innsender laster opp zip fil som inneholder forsendelse og får referanse til en kvittering. Kvitteringen får status OK i det filen er lastet opp til Altinn. For å vite om mottaker har klart å motta filen må innsender følge med på kvitteringsstatus og kvitteringstekst.

* ReceiptStatusEnum.OK og ReceiptText inneholder "Forsendelse mottatt og under behandling" - Mottaker har mottatt filen ok. Etter kvittering med denne statusen vil innsender få tilsendt forsendelsestatus som sier at den er mottatt ok og med innsendingsid, deretter forløpende filer med forsendelsestatus etterhvert som forsendelsen endrer status hos tinglysing.
* ReceiptStatusEnum.VALIDATION_FAILED - Forsendelsen feilet i mottak. Etter kvittering med denne statusen vil innsender få tilsendt en fil som inneholder forsendelsestatus med feilmelding.
* ReceiptStatusEnum.REJECTED - tinglysing klarte ikke å ta i mot filen, feilmelding gitt i kvittering. Innsender vil ikke tilsendt statusendringer på denne filen. 

![Innsending av fil via Altinn formidlingstjeneste](https://raw.githubusercontent.com/kartverket/eksempelklient-etinglysing-altinn/release/doc/altinn-opplasting.png)

### Nedlasting av tinglyingstatus
Innsender sjekker om det har kommet statusoppdatering på noen av forsendelsene man har sendt inn til Altinn ved å sjekke om det har kommet noen nye filer. Dersom det har kommet en forsendelsestatus vil man kunne matche dette mot innsendt forsendelse ved at de vil ha samme forsendelsereferanse. 
Dersom filen av en eller annen grunn ikke inneholder forsendelsereferanse må man koble tinglysingstatus til innsendt forsendelse gjennom sendersReference.
Forsendelsestatus vil alltid ha samme sendersReference som den innkommende filen. Her er det imidlertid ingen duplikat sjekk, innsender må derfor selv sørge for at denne er unik for hver innsendte fil dersom man skal kunne matche mottatte filer uten forsendelsesreferanse mot filer man har sendt fra seg. 

![Nedlasting av fil via Altinn formidlingstjeneste](https://raw.githubusercontent.com/kartverket/eksempelklient-etinglysing-altinn/release/doc/altinn-nedlasting.png)

### Innhold av filene

Når en fil skal sendes til Kartveket via Altinn så må den pakkes inn i en zip fil før den lastes opp til Altinn. 
Når Altinn mottar filen så pakkes den ut, og det blir lagt på en fil som heter manifest.xml. Denne filen inneholder informasjon om innholdet i zip filen, eksempelvis hvor mange filer det er og filnavn. 
Disse to filene blir på nytt lagt i en zip før den blir videresendt til mottaker.
Kartverket støtter kun innsending av en fil av gangen, dvs hver zip fil skal kun inneholde en forsendelse. Filen som pakkes inn i zip filen skal være en xml fil som inneholder forsendelse på formatet i innsending.xsd. 
For forsendelser til Kartverket støttes ISO-8859-1 encoding, Altinn spesifikke ting har UTF-16 encoding.

![Behandling av zip filer i Altinn formidlingstjeneste](https://raw.githubusercontent.com/kartverket/eksempelklient-etinglysing-altinn/release/doc/innhold-zip.png)

På samme måte som forsendelse som skal sendes inn til tinglysing pakkes inn i en zip fil, vil forsendelsesstatus som sendes tilbake fra Kartverket også være pakket inn i en zip fil. Zip filen vil inneholde manifest.xml (lagt på av Altinn) og en forsendelsesstatus på formatet definert i innsending.xsd.
Responsen fra Kartverket vil alltid kun inneholde en forsendelsesstatus i hver fil.

## Grensesnitt mot Kartverkets innsendingsservice
Dokumentasjon av InnsendingService i test: https://etgltest.grunnbok.noo/grunnbok/index.jsp
For innsendingservice finnes følgende tjenester som fortløpende vil bli støttet:

* `sendTilTinglysing`
* `valider` kan validere usignert og signert melding synkront for å se om denne er syntaksmessig og semantisk korrekt utfylt.
* `hentStatus` henter nåværende status for en gitt melding, inkludert eventuelle begrunnelser for meldinger som ikke kan tinglyses
   eller signerte grunnboksutskrifter for meldinger som har tinglyste dokumenter 

For å angi hvilken operasjon i innsendingstjenesten man skal kalle må man legge ved en property i det man initierer oversendelsen som heter 'operation' og sette den til en av disse verdiene: sendTilTinglysing, valider, hentStatus.
Dette må settes i altinn hvis ikke vil det bli returnert en feil fra Tinglysingen da man ikke vet hvilken operasjon man forsøker å kalle.

Requesten som sendes til InitiateBrokerService må da inneholde en dette:
        
        
        <ns1:PropertyList xmlns:ns1="http://schemas.altinn.no/services/ServiceEngine/Broker/2015/06">                         
            <ns1:Property>                  
                <ns1:PropertyKey>operation</ns1:PropertyKey>
                <ns1:PropertyValue>sendTilTinglysing</ns1:PropertyValue>
            </ns1:Property>
        </ns1:PropertyList>
        
# Annet

## Oppsett av systembruker i altinn

Hver enkelt sluttbruker må sette opp en systembruker for bruk av formidlingstjenesten i Sluttbrukerløsningen i Altinn. Dette er en forutsetning for at det skal være mulig å sende inn filer via formidlingstjenesten i Altinn. 
For test får man tildelt en testbruker med tilgang til en fiktiv organisasjon. Link til sluttbrukerløsningen i test: https://tt02.altinn.basefarm.net/
For å opprette en systembruker gjør følgende:

* Logg inn med en bruker med tilgang til organisasjonen du skal opprette systembruker for. Velg å representere organisasjonen.
* Gå til "Profil, roller og rettigheter", "Avanserte innstilinger" og "Datasystemer".
* Fyll ut Navn på systemet, Velg "Formidling" under datasystem og velg et passord
* Etter du har lagt til brukeren vil ID tilsvare systembruker (konfigparameter altinn.user), og det selvvalgte passordet vil tilsvare systempassord (konfigparameter altinn.password).
