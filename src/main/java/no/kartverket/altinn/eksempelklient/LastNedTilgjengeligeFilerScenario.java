package no.kartverket.altinn.eksempelklient;

import no.kartverket.altinn.eksempelklient.domain.AltinnForsendelse;
import no.kartverket.altinn.eksempelklient.domain.AltinnForsendelseResponse;
import no.kartverket.altinn.eksempelklient.service.AltinnService;

import java.util.List;
import java.util.Optional;

public class LastNedTilgjengeligeFilerScenario {

    private final AltinnService altinnService;
    private final String reportee;
    private List<AltinnForsendelse> altinnForsendelser;

    public LastNedTilgjengeligeFilerScenario(AltinnService altinnService, String reportee) {
        this.altinnService = altinnService;
        this.reportee = reportee;
    }


    public void execute(List<AltinnForsendelse> altinnForsendelser) {
        this.altinnForsendelser = altinnForsendelser;
        lastNedFilOgSkrivUtInnhold();
    }


    private void lastNedFilOgSkrivUtInnhold() {
        List<AltinnForsendelseResponse> tilgjengeligeFiler = altinnService.lastNedTilgjengeligeFiler(reportee);
        tilgjengeligeFiler.stream().forEach(AltinnForsendelseResponse::extractForsendelsestatusFraPayload);
        tilgjengeligeFiler.stream().forEach(this::matchMotInnsendtForsendelse);
    }


    private AltinnForsendelse matchMotInnsendtForsendelse(AltinnForsendelseResponse forsendelseResponse) {
        //Matcher først mot forsendelsereferanse satt i payload som blir sendt til elektronisk tinglysing. Denne blir det validert mot i elektronisk tinglysing at er unik pr forsendelse.
        AltinnForsendelse trackerInformation = matchMotForsendelsereferanseFraForsendelse(forsendelseResponse);
        //Dersom ingen match forsøk match mot sendersreference satt i manifest i BrokerServiceInitiation.
        if(trackerInformation == null) {
            trackerInformation = matchMotSendersReferenceFraAltinnManifest(forsendelseResponse);
        }
        if(trackerInformation != null) {
            trackerInformation.addForsendelseResponse(forsendelseResponse);
            System.out.println(String.format("Matchet nedlastet fil mot forsendelse med forsendelsereferanse: %s og sendersReference: %s", trackerInformation.getForsendelsereferanse(), trackerInformation.getAltinnTrackerInformation().getSendersReference()));
        }
        else {
            System.out.println(String.format("ERROR: Klarte ikke å matche response fil til innsendt tinglyst dokument. Forsendelsereferanse: %s, SendersReference: %S", forsendelseResponse.getForsendelsereferanse(), forsendelseResponse.getAltinnTrackerInformation().getSendersReference()));
        }
        forsendelseResponse.printForsendelsestatus();
        return trackerInformation;
    }


    private AltinnForsendelse matchMotForsendelsereferanseFraForsendelse(AltinnForsendelseResponse forsendelseResponse) {
        Optional<AltinnForsendelse> searchResult = altinnForsendelser.stream().filter(forsendelseResponse::erSvarPaa).findFirst();
        if(searchResult.isPresent()) {
            return searchResult.get();
        }
        return null;
    }

    private AltinnForsendelse matchMotSendersReferenceFraAltinnManifest(AltinnForsendelseResponse forsendelseResponse) {
        Optional<AltinnForsendelse> sendersReferenceSearchResult = altinnForsendelser.stream().filter(forsendelse -> forsendelseResponse.getAltinnTrackerInformation().harSammeSendersReference(forsendelse.getAltinnTrackerInformation())).findFirst();
        if(sendersReferenceSearchResult.isPresent()) {
            return sendersReferenceSearchResult.get();
        }
        return null;
    }


}
