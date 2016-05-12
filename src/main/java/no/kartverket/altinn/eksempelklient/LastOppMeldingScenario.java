package no.kartverket.altinn.eksempelklient;

import no.altinn.services.streamed.IBrokerServiceExternalBasicStreamedUploadFileStreamedBasicAltinnFaultFaultFaultMessage;
import no.kartverket.altinn.eksempelklient.domain.AltinnForsendelse;
import no.kartverket.altinn.eksempelklient.service.AltinnService;

public class LastOppMeldingScenario {

    private final AltinnService altinnService;
    private final String reportee;
    private final String recipient;

    public LastOppMeldingScenario(AltinnService altinnService, String reportee, String recipient) {
        this.altinnService = altinnService;
        this.reportee = reportee;
        this.recipient = recipient;
    }

    public void execute(AltinnForsendelse altinnForsendelse) {
        String fileReference = altinnService.initiateBrokerService(altinnForsendelse.getAltinnTrackerInformation().getSendersReference(), reportee, recipient);
        altinnForsendelse.getAltinnTrackerInformation().setFileReference(fileReference);
        System.out.println("Initiert opplasting av fil, filreferanse: " + fileReference);
        uploadFileToAltinn(altinnForsendelse);
    }


    private void uploadFileToAltinn(AltinnForsendelse altinnForsendelse) {
        try {
            Integer receiptId = altinnService.uploadFileToAltinn(altinnForsendelse, reportee);
            System.out.println(String.format("Fil med filreferanse: %s og kvitteringsid: %s lastet opp til Altinn ", altinnForsendelse.getAltinnTrackerInformation().getFileReference(), receiptId));
            System.out.println("");
            altinnForsendelse.getAltinnTrackerInformation().setReceiptId(receiptId);
            altinnForsendelse.setForsendelseRequestStatus(AltinnForsendelse.ForsendelseRequestStatus.SENT);
        } catch (IBrokerServiceExternalBasicStreamedUploadFileStreamedBasicAltinnFaultFaultFaultMessage message) {
            altinnForsendelse.setForsendelseRequestStatus(AltinnForsendelse.ForsendelseRequestStatus.FEILET);
            throw new RuntimeException("ERROR: Feil ved opplasting av fil til Altinn", message);
        }
    }


}

