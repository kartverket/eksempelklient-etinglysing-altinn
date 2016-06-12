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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.nio.file.Files;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class AltinnForsendelseResponse implements Comparable<AltinnForsendelseResponse>{

    private final String operationPropertyKey = "operation";

    private byte [] zipPayload;

    //setter disse verdiene etter å ha pakket ut zipfilen
    private String fileName;
    private byte [] forsendelseResponsePayload;
    private byte [] manifestPayload;
    private Forsendelsesstatus forsendelsesstatus;
    private InnsendingOperation innsendingOperation;

    private final AltinnTrackerInformation altinnTrackerInformation;

    public AltinnForsendelseResponse(AltinnTrackerInformation altinnTrackerInformation) {
        this.altinnTrackerInformation = altinnTrackerInformation;
    }

    public void extractResponseFromZip(byte[] zipPayload) {
        this.zipPayload = zipPayload;
        AltinnFileExtractor altinnFileExtractor = new AltinnFileExtractor(altinnInbound(new ByteArrayInputStream(zipPayload)));
        this.fileName = altinnFileExtractor.getName();
        this.forsendelseResponsePayload= altinnFileExtractor.getTargetRaw();
        forsendelsesstatus = extractForsendelsestatusFraPayload();
        this.manifestPayload= altinnFileExtractor.getManifestTargetRaw();
        innsendingOperation = extractInnsendingOperationFraManifestPayload();

    }

    private File altinnInbound(InputStream in) {
        try {
            File tempDir = Files.createTempDirectory("").toFile();
            final File inbound = Files.createTempFile(tempDir.toPath(), "", "").toFile();
            try(OutputStream out = new FileOutputStream(inbound)) {
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
            @SuppressWarnings("unused")
            JAXBElement<Forsendelsesstatus> unmarshalledObject = (JAXBElement<Forsendelsesstatus>) unmarshaller.unmarshal(new ByteArrayInputStream(forsendelseResponsePayload));
            return unmarshalledObject.getValue();
        } catch (JAXBException e) {
            System.out.println("ERROR: Kunne ikke hente ut forsendelsestatus fra forsendelse response for fil med filreferanse: " + altinnTrackerInformation.getFileReference());
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
            BrokerServiceManifest brokerServiceManifest= (BrokerServiceManifest) unmarshaller.unmarshal(new ByteArrayInputStream(manifestPayload));
            return getOperation(brokerServiceManifest);
        } catch (JAXBException e) {
            System.out.println(String.format("ERROR: Kunne ikke hente ut innsendingOperation fra forsendelse response for fil med filreferanse: %s", altinnTrackerInformation.getFileReference()));
            e.printStackTrace();
            return null;
        }
    }

    private InnsendingOperation getOperation(BrokerServiceManifest brokerServiceManifest) {
        if(brokerServiceManifest!=null && brokerServiceManifest.getPropertyList()!=null && brokerServiceManifest.getPropertyList().getProperty()!=null) {
            List<BrokerServiceManifest.PropertyList.Property> properties = brokerServiceManifest.getPropertyList().getProperty();
            for (BrokerServiceManifest.PropertyList.Property property : properties) {
                if (operationPropertyKey.equals(property.getPropertyKey())) {
                    String operationAsString = property.getPropertyValue();
                    System.out.println("Response inneholder manifest.xml med operation = " + operationAsString);
                    return InnsendingOperation.valueOf(operationAsString);
                }
            }
        }
        System.out.println("Response mangler property for operation i manifest.xml");
        return null;
    }

    public boolean erSvarPaa(AltinnForsendelse forsendelse) {
        if(innsendingOperation!=null && !innsendingOperation.equals(forsendelse.getOperation())) {
            return false;
        }
        if(InnsendingOperation.hentStatus.equals(forsendelse.getOperation())) {
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
        return forsendelsesstatus!=null?forsendelsesstatus.getForsendelsesreferanse():null;
    }

    private String getInnsendingsId() {
        return forsendelsesstatus!=null?forsendelsesstatus.getInnsendingId():null;
    }

    public AltinnTrackerInformation getAltinnTrackerInformation() {
        return altinnTrackerInformation;
    }

    public void printForsendelsestatus() {
        if(forsendelsesstatus==null) {
            System.out.println("Response inneholder ingen forsendelsestatus");
            System.out.println("");
            return;
        }
        System.out.println(forsendelsesstatus.getForsendelsesreferanse()!=null ?"Oppsummering av forsendelsestatus med forsendelsereferanse: "+ forsendelsesstatus.getForsendelsesreferanse(): "Mottok forsendelsestatus uten forsendelsereferanse");
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
                System.out.println("   Payload response: " + new String (getForsendelseResponsePayload()));
        }
    }


    private void printForsendelsestatusValider() {
        if(forsendelsesstatus.getBehandlingsinformasjon() == null || forsendelsesstatus.getBehandlingsinformasjon().getKontrollresultater() == null) {
            System.out.println("   Resultat: Validering OK");
        } else if (forsendelsesstatus.getBehandlingsinformasjon()!=null &&  forsendelsesstatus.getBehandlingsinformasjon().getKontrollresultater() !=null
                &&forsendelsesstatus.getBehandlingsinformasjon().getKontrollresultater().getKontrollresultat()!=null) {
            forsendelsesstatus.getBehandlingsinformasjon().getKontrollresultater().getKontrollresultat().stream().forEach(this::printKontrollresultat);
            System.out.println("");
        } else {
            System.out.println("   Ukjent tolkning av resultat " + new String(forsendelseResponsePayload));
        }
    }

    public void printForsendelsestatusSendTilTinglysing() {
        System.out.println("Innsendingsid: " + forsendelsesstatus.getInnsendingId());
        System.out.println(getPrioriteringstidspunktOgStatusAsString());
        //Forventer å ha behandlingsinformasjon med kontroll resultater dersom behandlingsutfall er UAVKLART, FORELOEPIG_NEKTET; ANKET ELLER AVVIST
        if(forsendelsesstatus.getBehandlingsinformasjon()!=null && forsendelsesstatus.getBehandlingsinformasjon().getKontrollresultater()!=null) {
            forsendelsesstatus.getBehandlingsinformasjon().getKontrollresultater().getKontrollresultat().stream().forEach(this::printKontrollresultat);
        }
        System.out.println("");
    }

    public void printForsendelsestatusHentStatus() {
        if(forsendelsesstatus.getInnsendingId()==null) {
            System.out.println("Fant ikke forsendelse for gitt innsendingsid");
        } else {
            System.out.println("Innsendingsid: " + forsendelsesstatus.getInnsendingId());
        }
        System.out.println("Prioriteringstidspunkt: "+forsendelsesstatus.getRegistreringstidspunkt() + ", Behandlingsutfall: "+forsendelsesstatus.getBehandlingsutfall() + ", Saksstatus: " + forsendelsesstatus.getSaksstatus());
        if(forsendelsesstatus.getBehandlingsinformasjon()!=null && forsendelsesstatus.getBehandlingsinformasjon().getKontrollresultater()!=null) {
            forsendelsesstatus.getBehandlingsinformasjon().getKontrollresultater().getKontrollresultat().stream().forEach(this::printKontrollresultat);
        }
        System.out.println("");
    }

    private void printKontrollresultat(Kontrollresultat kontrollresultat) {
        StringBuilder stringBuilder = new StringBuilder("   Kontrollresultat ");
        if(kontrollresultat.getDokumentindeks()!=null) {
            stringBuilder.append("for dokumentindeks ").append(kontrollresultat.getDokumentindeks())
                    .append(" og rettstiftelsesindeks ").append(kontrollresultat.getRettsstiftelsesindeks()).append(":");
        }
        stringBuilder.append(kontrollresultat.getNavn()).append(", utfall: ").append(kontrollresultat.getUtfall());
        System.out.println(stringBuilder.toString());
        kontrollresultat.getBegrunnelser().getBegrunnelse().stream().forEach(this::printBegrunnelser);
    }

    private void printBegrunnelser(Begrunnelse begrunnelse) {
        System.out.println("        Begrunnelse: "+begrunnelse.getTekst() +" ("+ begrunnelse.getElementnavn() + ")");
    }


    public DateTime getPrioriteringstidspunkt() {
        if(forsendelsesstatus!=null && forsendelsesstatus.getRegistreringstidspunkt()!=null) {
            return new DateTime(forsendelsesstatus.getRegistreringstidspunkt().toGregorianCalendar().getTime());
        }
        return null;
    }

    @Override
    public int compareTo(AltinnForsendelseResponse other) {
        Ordering<DateTime> NULLSLAST_ORDER =
                Ordering.<DateTime>natural().nullsLast();

        return NULLSLAST_ORDER.compare(this.getPrioriteringstidspunkt(), other.getPrioriteringstidspunkt());
    }

    public String getPrioriteringstidspunktOgStatusAsString() {
        if(forsendelsesstatus.getRegistreringstidspunkt()== null && forsendelsesstatus.getBehandlingsutfall()==null) {
            return "Mottatt response uten prioriteringstidspunkt og behandlingsutfall. Denne er AVVIST.";
        }
        return "Prioriteringstidspunkt: "+forsendelsesstatus.getRegistreringstidspunkt() + ", Behandlingsutfall: "+forsendelsesstatus.getBehandlingsutfall() + ", Saksstatus: " + forsendelsesstatus.getSaksstatus();
    }
}
