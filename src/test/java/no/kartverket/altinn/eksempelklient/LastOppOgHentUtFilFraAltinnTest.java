package no.kartverket.altinn.eksempelklient;


import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptExternal;
import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum;
import no.altinn.services.streamed.IBrokerServiceExternalBasicStreamedUploadFileStreamedBasicAltinnFaultFaultFaultMessage;
import no.kartverket.altinn.eksempelklient.domain.AltinnForsendelse;
import no.kartverket.altinn.eksempelklient.domain.AltinnForsendelseResponse;
import no.kartverket.altinn.eksempelklient.service.AltinnService;
import no.kartverket.altinn.eksempelklient.service.AltinnServiceFactory;
import no.kartverket.altinn.eksempelklient.service.ServiceParameters;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.*;

/**
 * Denne testen sjekker at man er i stand til å laste opp en fil til altinn med seg selv som mottaker og at man kan hente ned den samme filen igjen.
 * Verifiserer at grensesnittet mot altinn ikke har endret seg.
 */
public class LastOppOgHentUtFilFraAltinnTest {

    private AltinnService altinnService;
    private String reportee;

    @Before
    public void before() {
        ServiceParameters parameters = new ServiceParameters(System.getProperties());
        reportee = parameters.getReportee();

        AltinnServiceFactory serviceFactory = new AltinnServiceFactory(parameters.getSeverAddress());
        altinnService = new AltinnService(serviceFactory, parameters.getSystemUserName(), parameters.getSystemPassword(), parameters.getServiceCode(), parameters.getServiceEdtionCode());
    }

    @Test
    public void lastOppFilTilSegSelvOgHentNedSammeFil(){
        AltinnForsendelse altinnForsendelse = new AltinnForsendelse("/eksempelfiler/pant.xml", "ref");
        //Sender fil til seg selv..
        String filreference = altinnService.initiateBrokerService(altinnForsendelse.getAltinnTrackerInformation().getSendersReference(), reportee, reportee);
        altinnForsendelse.getAltinnTrackerInformation().setFileReference(filreference);
        Integer receiptId = null;
        try {
            receiptId = altinnService.uploadFileToAltinn(altinnForsendelse, reportee);
        } catch (IBrokerServiceExternalBasicStreamedUploadFileStreamedBasicAltinnFaultFaultFaultMessage e) {
            fail("Fikk ikke lastet opp fil til altinn: "+ e.getMessage());
        }

        //Henter kvittering:
        ReceiptExternal kvittering = altinnService.downloadReceipt(receiptId);
        assertEquals(ReceiptStatusEnum.OK, kvittering.getReceiptStatusCode());
        assertFalse(kvittering.getReceiptText().getValue().startsWith(VentPaaKvitteringFraMottakerScenario.KVITTERINGTEKST_MOTTATT_OK));


        //Laster ned samme fil og oppdater kvittering..
        List<AltinnForsendelseResponse> tilgjengeligeFiler = altinnService.lastNedTilgjengeligeFiler(reportee);
        assertEquals(1, tilgjengeligeFiler.size());
        AltinnForsendelseResponse nedlastetFil = tilgjengeligeFiler.iterator().next();
        assertEquals(altinnForsendelse.getFileName(), nedlastetFil.getFileName());
        assertEquals(altinnForsendelse.getAltinnTrackerInformation().getSendersReference(), nedlastetFil.getAltinnTrackerInformation().getSendersReference());
        assertEquals(altinnForsendelse.getAltinnTrackerInformation().getFileReference(), nedlastetFil.getAltinnTrackerInformation().getFileReference());

        //Sjekker at kvittering sier at nå er filen lastet ned av mottaker
        kvittering = altinnService.downloadReceipt(receiptId);
        assertEquals(ReceiptStatusEnum.OK, kvittering.getReceiptStatusCode());
        checkState(kvittering.getSubReceipts().getValue().getReceiptExternal().size()==1, "Forventer kun en subreceipt fordi vi kun har sendt filen til en mottaker");
        ReceiptExternal receiptFromRecepient = kvittering.getSubReceipts().getValue().getReceiptExternal().iterator().next();
        assertTrue(receiptFromRecepient.getReceiptText().getValue(), receiptFromRecepient.getReceiptText().getValue().contains(AltinnService.OK_RECEIPT_TEXT));
    }

}
