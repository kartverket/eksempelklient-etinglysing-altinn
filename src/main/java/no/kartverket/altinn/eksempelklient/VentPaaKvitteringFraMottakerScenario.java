package no.kartverket.altinn.eksempelklient;

import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum;
import no.altinn.schemas.services.intermediary.receipt._2015._06.Receipt;
import no.kartverket.altinn.eksempelklient.domain.AltinnForsendelse;
import no.kartverket.altinn.eksempelklient.service.AltinnService;

import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static no.kartverket.altinn.eksempelklient.domain.AltinnForsendelse.ForsendelseRequestStatus;

public class VentPaaKvitteringFraMottakerScenario {

    static final String KVITTERINGTEKST_MOTTATT_OK = "Forsendelse mottatt og under behandling";

    private final AltinnService altinnService;

    public VentPaaKvitteringFraMottakerScenario(AltinnService altinnService) {
        this.altinnService = altinnService;
    }

    public void execute(AltinnForsendelse altinnForsendelse) {
        List<ForsendelseRequestStatus> mottattEllerFeiletStatuser = Arrays.asList(ForsendelseRequestStatus.FEILET, ForsendelseRequestStatus.MOTTATT, ForsendelseRequestStatus.VALIDATION_FAILED);
        while (!mottattEllerFeiletStatuser.contains(altinnForsendelse.getForsendelseRequestStatus())) {
            Receipt receipt = altinnService.downloadReceipt(altinnForsendelse.getAltinnTrackerInformation().getReceiptId());
            if (ForsendelseRequestStatus.SENT.equals(altinnForsendelse.getForsendelseRequestStatus()) && ReceiptStatusEnum.OK.equals(receipt.getReceiptStatus())) {
                System.out.println(String.format("Mottatt kvittering på at fil med %s lastet opp til Altinn: %s", altinnForsendelse.getIdentificationAsString(), receipt.getReceiptText().getValue()));
                altinnForsendelse.setForsendelseRequestStatus(ForsendelseRequestStatus.UPLOADED_OK);
            }
            if (ForsendelseRequestStatus.UPLOADED_OK.equals(altinnForsendelse.getForsendelseRequestStatus()) && receipt.getSubReceipts().getValue() != null && receipt.getSubReceipts().getValue().getReceipt().size() > 0) {
                checkState(receipt.getSubReceipts().getValue().getReceipt().size() == 1, "Forventer kun en subreceipt fordi vi kun har sendt filen til en mottaker");
                Receipt receiptFromRecepient = receipt.getSubReceipts().getValue().getReceipt().iterator().next();
                if (ReceiptStatusEnum.OK.equals(receiptFromRecepient.getReceiptStatus()) && receiptFromRecepient.getReceiptText().getValue().contains(KVITTERINGTEKST_MOTTATT_OK)) {
                    altinnForsendelse.setForsendelseRequestStatus(ForsendelseRequestStatus.MOTTATT);
                    //I dette tilfellet vil forsendelseresponse bli sendt tilbake i egen fil
                    System.out.println(String.format("Mottatt kvittering på at forsendelse med %s mottatt i elektronsik tinglysing: %s", altinnForsendelse.getIdentificationAsString(), receiptFromRecepient.getReceiptText().getValue()));
                    System.out.println("");
                }
                if (ReceiptStatusEnum.VALIDATION_FAILED.equals(receiptFromRecepient.getReceiptStatus())) {
                    altinnForsendelse.setForsendelseRequestStatus(ForsendelseRequestStatus.VALIDATION_FAILED);
                    //I dette tilfellet vil forsendelseresponse bli sendt tilbake i egen fil
                    System.out.println(String.format("Mottatt kvittering på at forsendelse med %s mottatt men feilet i elektronsik tinglysing: %s", altinnForsendelse.getIdentificationAsString(), receiptFromRecepient.getReceiptText().getValue()));
                    System.out.println("");
                }
                if (ReceiptStatusEnum.REJECTED.equals(receiptFromRecepient.getReceiptStatus())) {
                    altinnForsendelse.setForsendelseRequestStatus(ForsendelseRequestStatus.FEILET);
                    //Her vil det ikke komme noen filer tilbake fra mottaker
                    System.out.println(String.format("ERROR: Mottatt kvittering på at forsendelse med %s feilet ved innsending til elektronisk tinglysing med feilmelding: %s", altinnForsendelse.getIdentificationAsString(), receiptFromRecepient.getReceiptText().getValue()));
                    System.out.println("");
                }
            }
        }
    }


}
