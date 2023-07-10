package no.kartverket.altinn.eksempelklient;

import no.kartverket.altinn.eksempelklient.domain.AltinnForsendelse;
import no.kartverket.altinn.eksempelklient.domain.AltinnForsendelseResponse;
import no.kartverket.altinn.eksempelklient.service.AltinnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class LastNedTilgjengeligeFilerScenario {

    private static final Logger log = LoggerFactory.getLogger(LastNedTilgjengeligeFilerScenario.class);
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
        tilgjengeligeFiler.forEach(this::matchMotInnsendtForsendelse);
    }


    private AltinnForsendelse matchMotInnsendtForsendelse(AltinnForsendelseResponse forsendelseResponse) {
        // Matcher først mot forsendelsereferanse satt i payload som blir sendt til elektronisk tinglysing. Denne blir det validert mot i elektronisk tinglysing at er unik pr forsendelse.
        AltinnForsendelse matchendeForsendelseRequest = matchMotForsendelsereferanseFraForsendelse(forsendelseResponse);

        // Dersom ingen match forsøk match mot sendersreference satt i manifest i BrokerServiceInitiation.
        if (matchendeForsendelseRequest == null) {
            matchendeForsendelseRequest = matchMotSendersReferenceFraAltinnManifest(forsendelseResponse);
        }

        if (matchendeForsendelseRequest != null) {
            matchendeForsendelseRequest.addForsendelseResponse(forsendelseResponse);
            log.info("Matchet nedlastet fil mot forsendelse for {} med {} og sendersReference: {}", matchendeForsendelseRequest.getOperation().name(), matchendeForsendelseRequest.getIdentificationAsString(), matchendeForsendelseRequest.getAltinnTrackerInformation().getSendersReference());
        } else {
            log.error("Klarte ikke å matche response fil til innsendt tinglyst dokument. Forsendelsereferanse: {}, SendersReference: {}", forsendelseResponse.getForsendelsereferanse(), forsendelseResponse.getAltinnTrackerInformation().getSendersReference());
        }

        forsendelseResponse.printForsendelsestatus();

        return matchendeForsendelseRequest;
    }


    private AltinnForsendelse matchMotForsendelsereferanseFraForsendelse(AltinnForsendelseResponse forsendelseResponse) {
        Optional<AltinnForsendelse> searchResult = altinnForsendelser.stream()
                .filter(forsendelseResponse::erSvarPaa)
                .findFirst();
        return searchResult.orElse(null);
    }

    private AltinnForsendelse matchMotSendersReferenceFraAltinnManifest(AltinnForsendelseResponse forsendelseResponse) {
        Optional<AltinnForsendelse> sendersReferenceSearchResult = altinnForsendelser.stream()
                .filter(forsendelse -> forsendelseResponse.getAltinnTrackerInformation().harSammeSendersReference(forsendelse.getAltinnTrackerInformation()))
                .findFirst();
        return sendersReferenceSearchResult.orElse(null);
    }


}
