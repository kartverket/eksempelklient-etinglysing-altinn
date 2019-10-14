package no.kartverket.altinn.eksempelklient;

import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum;
import no.altinn.schemas.services.intermediary.receipt._2015._06.Receipt;
import no.kartverket.altinn.eksempelklient.domain.AltinnForsendelse;
import no.kartverket.altinn.eksempelklient.domain.ForsendelseRequestStatus;
import no.kartverket.altinn.eksempelklient.service.AltinnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;

public class VentPaaKvitteringFraMottakerScenario {

    private static final String KVITTERINGTEKST_MOTTATT_OK = "Forsendelse mottatt og under behandling";
    private static final List<ForsendelseRequestStatus> mottattEllerFeiletStatuser = Arrays.asList(
            ForsendelseRequestStatus.FEILET,
            ForsendelseRequestStatus.MOTTATT,
            ForsendelseRequestStatus.VALIDATION_FAILED
    );

    private final Logger log = LoggerFactory.getLogger(VentPaaKvitteringFraMottakerScenario.class);
    private final AltinnService altinnService;

    public VentPaaKvitteringFraMottakerScenario(AltinnService altinnService) {
        this.altinnService = altinnService;
    }

    public void execute(AltinnForsendelse altinnForsendelse) {
        while (!mottattEllerFeiletStatuser.contains(altinnForsendelse.getForsendelseRequestStatus())) {
            Receipt receipt = altinnService.downloadReceipt(altinnForsendelse.getAltinnTrackerInformation().getReceiptId());

            if (ForsendelseRequestStatus.SENT.equals(altinnForsendelse.getForsendelseRequestStatus()) && ReceiptStatusEnum.OK.equals(receipt.getReceiptStatus())) {
                log.info("Mottatt kvittering på at fil med {} lastet opp til Altinn: {}", altinnForsendelse.getIdentificationAsString(), receipt.getReceiptText().getValue());
                altinnForsendelse.setForsendelseRequestStatus(ForsendelseRequestStatus.UPLOADED_OK);
            }

            if (ForsendelseRequestStatus.UPLOADED_OK.equals(altinnForsendelse.getForsendelseRequestStatus())
                    && receipt.getSubReceipts().getValue() != null
                    && receipt.getSubReceipts().getValue().getReceipt().size() > 0) {

                checkState(receipt.getSubReceipts().getValue().getReceipt().size() == 1, "Forventer kun en subreceipt fordi vi kun har sendt filen til en mottaker");

                Receipt receiptFromRecepient = receipt.getSubReceipts().getValue().getReceipt().iterator().next();
                if (ReceiptStatusEnum.OK.equals(receiptFromRecepient.getReceiptStatus())
                        && receiptFromRecepient.getReceiptText().getValue().contains(KVITTERINGTEKST_MOTTATT_OK)) {
                    altinnForsendelse.setForsendelseRequestStatus(ForsendelseRequestStatus.MOTTATT);
                    // I dette tilfellet vil forsendelseresponse bli sendt tilbake i egen fil
                    log.info("Mottatt kvittering på at forsendelse med {} mottatt i elektronisk tinglysing: {}", altinnForsendelse.getIdentificationAsString(), receiptFromRecepient.getReceiptText().getValue());
                }
                if (ReceiptStatusEnum.REJECTED.equals(receiptFromRecepient.getReceiptStatus())) {
                    altinnForsendelse.setForsendelseRequestStatus(ForsendelseRequestStatus.FEILET);
                    // Her vil det ikke komme noen filer tilbake fra mottaker
                    log.error("Mottatt kvittering på at forsendelse med {} feilet ved innsending til elektronisk tinglysing med feilmelding: {}", altinnForsendelse.getIdentificationAsString(), receiptFromRecepient.getReceiptText().getValue());
                }
            }
        }
    }


}
