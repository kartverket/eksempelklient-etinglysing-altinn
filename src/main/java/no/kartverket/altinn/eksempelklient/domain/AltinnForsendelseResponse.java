package no.kartverket.altinn.eksempelklient.domain;

import com.google.common.collect.Ordering;
import com.google.common.io.ByteStreams;
import no.altinn.services.serviceengine.broker.BrokerServiceManifest;
import no.kartverket.altinn.eksempelklient.AltinnFileExtractor;
import no.kartverket.grunnbok.wsapi.v2.domain.innsending.Begrunnelse;
import no.kartverket.grunnbok.wsapi.v2.domain.innsending.Forsendelsesstatus;
import no.kartverket.grunnbok.wsapi.v2.domain.innsending.Kontrollresultat;
import no.kartverket.grunnbok.wsapi.v2.domain.innsending.ObjectFactory;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.nio.file.Files;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class AltinnForsendelseResponse implements Comparable<AltinnForsendelseResponse> {

    private static final String operationPropertyKey = "operation";

    private final Logger log = LoggerFactory.getLogger(AltinnForsendelseResponse.class);
    private final AltinnTrackerInformation altinnTrackerInformation;

    private String fileName;
    private Forsendelsesstatus forsendelsesstatus;
    private InnsendingOperation innsendingOperation;
    private byte[] zipPayload;
    private byte[] forsendelseResponsePayload;
    private byte[] manifestPayload;


    public AltinnForsendelseResponse(AltinnTrackerInformation altinnTrackerInformation) {
        this.altinnTrackerInformation = altinnTrackerInformation;
    }

    public void extractResponseFromZip(byte[] zipPayload) {
        AltinnFileExtractor altinnFileExtractor = new AltinnFileExtractor(altinnInbound(new ByteArrayInputStream(zipPayload)));
        this.fileName = altinnFileExtractor.getName();
        this.forsendelseResponsePayload = altinnFileExtractor.getTargetRaw();
        this.zipPayload = zipPayload;
        this.manifestPayload = altinnFileExtractor.getManifestTargetRaw();
        forsendelsesstatus = extractForsendelsestatusFraPayload();
        innsendingOperation = extractInnsendingOperationFraManifestPayload();

    }

    private File altinnInbound(InputStream in) {
        try {
            File tempDir = Files.createTempDirectory("").toFile();
            final File inbound = Files.createTempFile(tempDir.toPath(), "", "").toFile();
            try (OutputStream out = new FileOutputStream(inbound)) {
                ByteStreams.copy(in, out);
                return inbound;
            }
        } catch (IOException e) {
            throw new RuntimeException("ERROR: Feil ved opprettelse av temp fil ved nedlasting fra altinn", e);
        }
    }

    public Forsendelsesstatus extractForsendelsestatusFraPayload() {
        checkNotNull(forsendelseResponsePayload, "Kan ikke hente ut forsendelsereferanse uten å hente ut payload for response fra zipfilen først!");
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            JAXBElement<Forsendelsesstatus> unmarshalledObject = (JAXBElement<Forsendelsesstatus>) unmarshaller.unmarshal(new ByteArrayInputStream(forsendelseResponsePayload));
            return unmarshalledObject.getValue();
        } catch (JAXBException e) {
            log.error("Kunne ikke hente ut forsendelsestatus fra forsendelse response for fil med filreferanse: {}", altinnTrackerInformation.getFileReference());
            e.printStackTrace();
            return null;
        }
    }

    public InnsendingOperation extractInnsendingOperationFraManifestPayload() {
        checkNotNull(manifestPayload, "Kan ikke hente ut operasjon uten å hente ut payload for manifest fra zipfilen først!");
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(no.altinn.services.serviceengine.broker.ObjectFactory.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            @SuppressWarnings("unused")
            BrokerServiceManifest brokerServiceManifest = (BrokerServiceManifest) unmarshaller.unmarshal(new ByteArrayInputStream(manifestPayload));
            return getOperation(brokerServiceManifest);
        } catch (JAXBException e) {
            log.error("Kunne ikke hente ut innsendingOperation fra forsendelse response for fil med filreferanse: {}", altinnTrackerInformation.getFileReference());
            e.printStackTrace();
            return null;
        }
    }

    private InnsendingOperation getOperation(BrokerServiceManifest brokerServiceManifest) {
        if (brokerServiceManifest != null && brokerServiceManifest.getPropertyList() != null && brokerServiceManifest.getPropertyList().getProperty() != null) {
            List<BrokerServiceManifest.PropertyList.Property> properties = brokerServiceManifest.getPropertyList().getProperty();
            for (BrokerServiceManifest.PropertyList.Property property : properties) {
                if (operationPropertyKey.equals(property.getPropertyKey())) {
                    String operationAsString = property.getPropertyValue();
                    log.info("Response inneholder manifest.xml med operation = {}", operationAsString);
                    return InnsendingOperation.valueOf(operationAsString);
                }
            }
        }
        log.info("Response mangler property for operation i manifest.xml");
        return null;
    }

    public boolean erSvarPaa(AltinnForsendelse forsendelse) {
        if (innsendingOperation != null && !innsendingOperation.equals(forsendelse.getOperation())) {
            return false;
        }
        if (InnsendingOperation.hentStatus.equals(forsendelse.getOperation())) {
            return forsendelse.getInnsendingsId() != null && forsendelse.getInnsendingsId().equals(getInnsendingsId());
        } else {
            return forsendelse.getForsendelsereferanse() != null && forsendelse.getForsendelsereferanse().equals(getForsendelsereferanse());
        }
    }

    public byte[] getZipPayload() {
        return zipPayload;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getForsendelseResponsePayload() {
        return forsendelseResponsePayload;
    }

    public String getForsendelsereferanse() {
        return forsendelsesstatus != null ? forsendelsesstatus.getForsendelsesreferanse() : null;
    }

    private String getInnsendingsId() {
        return forsendelsesstatus != null ? forsendelsesstatus.getInnsendingId() : null;
    }

    public AltinnTrackerInformation getAltinnTrackerInformation() {
        return altinnTrackerInformation;
    }

    public void printForsendelsestatus() {
        if (forsendelsesstatus == null) {
            log.info("Response inneholder ingen forsendelsestatus");
            return;
        }

        if (forsendelsesstatus.getForsendelsesreferanse() != null)
            log.info("Oppsummering av forsendelsestatus med forsendelsereferanse: {}", forsendelsesstatus.getForsendelsesreferanse());
        else {
            log.info("Mottok forsendelsestatus uten forsendelsereferanse");
        }

        switch (innsendingOperation) {
            case sendTilTinglysing:
                printForsendelsestatusSendTilTinglysing();
                break;
            case hentStatus:
                printForsendelsestatusHentStatus();
                break;
            case valider:
                printForsendelsestatusValider();
                break;
            default:
                log.info("   Payload response: {}", new String(getForsendelseResponsePayload()));
        }
    }


    private void printForsendelsestatusValider() {
        if (forsendelsesstatus.getBehandlingsinformasjon() == null || forsendelsesstatus.getBehandlingsinformasjon().getKontrollresultater() == null) {
            log.info("   Resultat: Validering OK");
        } else if (forsendelsesstatus.getBehandlingsinformasjon() != null && forsendelsesstatus.getBehandlingsinformasjon().getKontrollresultater() != null
                && forsendelsesstatus.getBehandlingsinformasjon().getKontrollresultater().getKontrollresultat() != null) {
            forsendelsesstatus.getBehandlingsinformasjon().getKontrollresultater().getKontrollresultat().forEach(this::printKontrollresultat);
        } else {
            System.out.println("   Ukjent tolkning av resultat " + new String(forsendelseResponsePayload));
        }
    }

    public void printForsendelsestatusSendTilTinglysing() {
        log.info("Innsendingsid: {}", forsendelsesstatus.getInnsendingId());
        log.info(getPrioriteringstidspunktOgStatusAsString());

        // Forventer å ha behandlingsinformasjon med kontroll resultater dersom behandlingsutfall er UAVKLART, FORELOEPIG_NEKTET; ANKET ELLER AVVIST
        if (forsendelsesstatus.getBehandlingsinformasjon() != null && forsendelsesstatus.getBehandlingsinformasjon().getKontrollresultater() != null) {
            forsendelsesstatus.getBehandlingsinformasjon().getKontrollresultater().getKontrollresultat().forEach(this::printKontrollresultat);
        }
    }

    public void printForsendelsestatusHentStatus() {
        if (forsendelsesstatus.getInnsendingId() == null) {
            log.info("Fant ikke forsendelse for gitt innsendingsid");
        } else {
            log.info("Innsendingsid: {}", forsendelsesstatus.getInnsendingId());
        }

        log.info("Prioriteringstidspunkt: {}", forsendelsesstatus.getRegistreringstidspunkt());
        log.info("Behandlingsutfall: {}", forsendelsesstatus.getBehandlingsutfall());
        log.info("Saksstatus: {}", forsendelsesstatus.getSaksstatus());

        if (forsendelsesstatus.getBehandlingsinformasjon() != null && forsendelsesstatus.getBehandlingsinformasjon().getKontrollresultater() != null) {
            forsendelsesstatus.getBehandlingsinformasjon().getKontrollresultater().getKontrollresultat().forEach(this::printKontrollresultat);
        }
    }

    private void printKontrollresultat(Kontrollresultat kontrollresultat) {
        StringBuilder stringBuilder = new StringBuilder("   Kontrollresultat ");
        if (kontrollresultat.getDokumentindeks() != null) {
            stringBuilder
                    .append("for dokumentindeks ")
                    .append(kontrollresultat.getDokumentindeks())
                    .append(" og rettstiftelsesindeks ")
                    .append(kontrollresultat.getRettsstiftelsesindeks()).append(":");
        }

        stringBuilder
                .append(kontrollresultat.getNavn())
                .append(", utfall: ")
                .append(kontrollresultat.getUtfall());
        log.info(stringBuilder.toString());
        kontrollresultat.getBegrunnelser().getBegrunnelse().forEach(this::printBegrunnelser);
    }

    private void printBegrunnelser(Begrunnelse begrunnelse) {
        log.info("        Begrunnelse: " + begrunnelse.getTekst() + " (" + begrunnelse.getElementnavn() + ")");
    }


    public DateTime getPrioriteringstidspunkt() {
        if (forsendelsesstatus != null && forsendelsesstatus.getRegistreringstidspunkt() != null) {
            return new DateTime(forsendelsesstatus.getRegistreringstidspunkt().toGregorianCalendar().getTime());
        }
        return null;
    }

    @Override
    public int compareTo(AltinnForsendelseResponse other) {
        Ordering<DateTime> nullslastOrder = Ordering.<DateTime>natural().nullsLast();
        return nullslastOrder.compare(this.getPrioriteringstidspunkt(), other.getPrioriteringstidspunkt());
    }

    public String getPrioriteringstidspunktOgStatusAsString() {
        if (forsendelsesstatus.getRegistreringstidspunkt() == null && forsendelsesstatus.getBehandlingsutfall() == null) {
            return "Mottatt response uten prioriteringstidspunkt og behandlingsutfall. Denne er AVVIST.";
        }
        return "Prioriteringstidspunkt: " + forsendelsesstatus.getRegistreringstidspunkt() + ", Behandlingsutfall: " + forsendelsesstatus.getBehandlingsutfall() + ", Saksstatus: " + forsendelsesstatus.getSaksstatus();
    }
}
