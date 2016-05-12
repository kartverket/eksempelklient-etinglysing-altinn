package no.kartverket.altinn.eksempelklient.domain;

import com.google.common.collect.Ordering;
import com.google.common.io.ByteStreams;
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

import static com.google.common.base.Preconditions.checkNotNull;

public class AltinnForsendelseResponse implements Comparable<AltinnForsendelseResponse>{

    private byte [] zipPayload;

    //setter disse verdiene etter å ha pakket ut zipfilen
    private String fileName;
    private byte [] forsendelseResponsePayload;
    private Forsendelsesstatus forsendelsesstatus;

    private final AltinnTrackerInformation altinnTrackerInformation;

    public AltinnForsendelseResponse(AltinnTrackerInformation altinnTrackerInformation) {
        this.altinnTrackerInformation = altinnTrackerInformation;
    }

    public void extractResponseFromZip(byte[] zipPayload) {
        this.zipPayload = zipPayload;
        AltinnFileExtractor altinnFileExtractor = new AltinnFileExtractor(altinnInbound(new ByteArrayInputStream(zipPayload)));
        this.fileName = altinnFileExtractor.getName();
        this.forsendelseResponsePayload= altinnFileExtractor.getTargetRaw();
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

    public void extractForsendelsestatusFraPayload() {
        checkNotNull(forsendelseResponsePayload, "Kan ikke hente ut forsendelsereferanse uten å hente ut payload for response fra zipfilen først!");
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            @SuppressWarnings("unused")
            JAXBElement<Forsendelsesstatus> unmarshalledObject = (JAXBElement<Forsendelsesstatus>) unmarshaller.unmarshal(new ByteArrayInputStream(forsendelseResponsePayload));
            forsendelsesstatus = unmarshalledObject.getValue();
        } catch (JAXBException e) {
            System.out.println("ERROR: Kunne ikke hente ut forsendelsestatus fra forsendelse response for fil med filreferanse: " + altinnTrackerInformation.getFileReference());
        }
    }

    public boolean erSvarPaa(AltinnForsendelse forsendelse) {
        return forsendelse.getForsendelsereferanse() != null && forsendelse.getForsendelsereferanse().equals(getForsendelsereferanse());
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
        System.out.println("Innsendingsid: " + forsendelsesstatus.getInnsendingId());
        System.out.println(getPrioriteringstidspunktOgStatusAsString());
        //Forventer å ha behandlingsinformasjon med kontroll resultater dersom behandlingsutfall er UAVKLART, FORELOEPIG_NEKTET; ANKET ELLER AVVIST
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
