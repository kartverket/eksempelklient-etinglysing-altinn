package no.kartverket.altinn.eksempelklient;

import no.altinn.services.streamed.IBrokerServiceExternalBasicStreamedUploadFileStreamedBasicAltinnFaultFaultFaultMessage;
import no.kartverket.altinn.eksempelklient.domain.AltinnForsendelse;
import no.kartverket.altinn.eksempelklient.service.AltinnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LastOppMeldingScenario {

    private final Logger log = LoggerFactory.getLogger(LastOppMeldingScenario.class);
    private final AltinnService altinnService;
    private final String reportee;
    private final String recipient;

    public LastOppMeldingScenario(AltinnService altinnService, String reportee, String recipient) {
        this.altinnService = altinnService;
        this.reportee = reportee;
        this.recipient = recipient;
    }

    public void execute(AltinnForsendelse altinnForsendelse) {
        String fileReference = altinnService.initiateBrokerService(altinnForsendelse.getAltinnTrackerInformation().getSendersReference(), reportee, recipient, altinnForsendelse.getOperation());
        altinnForsendelse.getAltinnTrackerInformation().setFileReference(fileReference);
        log.info("Initiert opplasting av fil, filreferanse: {}", fileReference);
        uploadFileToAltinn(altinnForsendelse);
    }


    private void uploadFileToAltinn(AltinnForsendelse altinnForsendelse) {
        try {
            Integer receiptId = altinnService.uploadFileToAltinn(altinnForsendelse, reportee);
            log.info("Fil med filreferanse: {} og kvitteringsid: {} lastet opp til Altinn for {}", altinnForsendelse.getAltinnTrackerInformation().getFileReference(), receiptId, altinnForsendelse.getOperation());
            altinnForsendelse.getAltinnTrackerInformation().setReceiptId(receiptId);
            altinnForsendelse.setForsendelseRequestStatus(AltinnForsendelse.ForsendelseRequestStatus.SENT);
        } catch (IBrokerServiceExternalBasicStreamedUploadFileStreamedBasicAltinnFaultFaultFaultMessage message) {
            altinnForsendelse.setForsendelseRequestStatus(AltinnForsendelse.ForsendelseRequestStatus.FEILET);
            throw new RuntimeException("ERROR: Feil ved opplasting av fil til Altinn", message);
        }
    }


}

