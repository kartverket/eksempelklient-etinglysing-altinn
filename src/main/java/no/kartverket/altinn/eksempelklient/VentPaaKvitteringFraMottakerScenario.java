package no.kartverket.altinn.eksempelklient;

import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptExternal;
import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum;
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
            ReceiptExternal receipt = altinnService.downloadReceipt(altinnForsendelse.getAltinnTrackerInformation().getReceiptId());
            if (ForsendelseRequestStatus.SENT.equals(altinnForsendelse.getForsendelseRequestStatus()) && ReceiptStatusEnum.OK.equals(receipt.getReceiptStatusCode())) {
                System.out.println(String.format("Mottatt kvittering på at fil med forsendelsereferanse %s lastet opp til Altinn: %s", altinnForsendelse.getForsendelsereferanse(), receipt.getReceiptText().getValue()));
                altinnForsendelse.setForsendelseRequestStatus(ForsendelseRequestStatus.UPLOADED_OK);
            }
            if (ForsendelseRequestStatus.UPLOADED_OK.equals(altinnForsendelse.getForsendelseRequestStatus()) && receipt.getSubReceipts().getValue() != null && receipt.getSubReceipts().getValue().getReceiptExternal().size() > 0) {
                checkState(receipt.getSubReceipts().getValue().getReceiptExternal().size() == 1, "Forventer kun en subreceipt fordi vi kun har sendt filen til en mottaker");
                ReceiptExternal receiptFromRecepient = receipt.getSubReceipts().getValue().getReceiptExternal().iterator().next();
                if (ReceiptStatusEnum.OK.equals(receiptFromRecepient.getReceiptStatusCode()) && receiptFromRecepient.getReceiptText().getValue().contains(KVITTERINGTEKST_MOTTATT_OK)) {
                    altinnForsendelse.setForsendelseRequestStatus(ForsendelseRequestStatus.MOTTATT);
                    //I dette tilfellet vil forsendelseresponse bli sendt tilbake i egen fil
                    System.out.println(String.format("Mottatt kvittering på at forsendelse med forsendelsereferanse %s mottatt i elektronsik tinglysing: %s", altinnForsendelse.getForsendelsereferanse(), receiptFromRecepient.getReceiptText().getValue()));
                    System.out.println("");
                }
                if (ReceiptStatusEnum.VALIDATION_FAILED.equals(receiptFromRecepient.getReceiptStatusCode())) {
                    altinnForsendelse.setForsendelseRequestStatus(ForsendelseRequestStatus.VALIDATION_FAILED);
                    //I dette tilfellet vil forsendelseresponse bli sendt tilbake i egen fil
                    System.out.println(String.format("Mottatt kvittering på at forsendelse med forsendelsereferanse %s mottatt men feilet i elektronsik tinglysing: %s", altinnForsendelse.getForsendelsereferanse(), receiptFromRecepient.getReceiptText().getValue()));
                    System.out.println("");
                }
                if (ReceiptStatusEnum.REJECTED.equals(receiptFromRecepient.getReceiptStatusCode())) {
                    altinnForsendelse.setForsendelseRequestStatus(ForsendelseRequestStatus.FEILET);
                    //Her vil det ikke komme noen filer tilbake fra mottaker
                    System.out.println(String.format("ERROR: Mottatt kvittering på at forsendelse med forsendelsereferanse %s feilet ved innsending til elektronisk tinglysing med feilmelding: %s", altinnForsendelse.getForsendelsereferanse(), receiptFromRecepient.getReceiptText().getValue()));
                    System.out.println("");
                }
            }
        }
    }


}
