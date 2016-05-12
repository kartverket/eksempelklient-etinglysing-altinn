package no.kartverket.altinn.eksempelklient.domain;

import com.google.common.io.ByteStreams;
import no.kartverket.altinn.eksempelklient.AltinnFileCreator;
import no.kartverket.grunnbok.wsapi.v2.domain.innsending.Forsendelse;
import no.kartverket.grunnbok.wsapi.v2.domain.innsending.ObjectFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AltinnForsendelse {

    public enum ForsendelseRequestStatus {SENT, UPLOADED_OK, MOTTATT, VALIDATION_FAILED, FEILET}

    private ForsendelseRequestStatus forsendelseRequestStatus;
    private final String fileName;
    private final byte [] forsendelsePayload;

    private final List<AltinnForsendelseResponse> responses = new ArrayList<>();

    //referanse satt av innsender i forsendelsePayload, unik pr forsendelse
    private final String forsendelsereferanse;

    //Altinn referanser
    private final AltinnTrackerInformation altinnTrackerInformation = new AltinnTrackerInformation();

    public AltinnForsendelse(String filename, String sendersReference) {
        this.fileName = filename;
        this.forsendelsePayload = getPayload(filename);
        this.altinnTrackerInformation.setSendersReference(sendersReference);
        this.forsendelsereferanse = extractForsendelsereferanseFraPayload();
    }

    public byte[] pakkInnIZipFil() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        new AltinnFileCreator(fileName, forsendelsePayload, outputStream);
        return outputStream.toByteArray();
    }

    private byte[] getPayload(String fileName) {
        try {
            return ByteStreams.toByteArray(getClass().getResourceAsStream(fileName));
        } catch (IOException e) {
            throw new RuntimeException("ERROR: Could not read payload from " +fileName);
        }
    }

    private String extractForsendelsereferanseFraPayload() {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            @SuppressWarnings("unchecked")
            JAXBElement<Forsendelse> unmarshalledObject = (JAXBElement<Forsendelse>) unmarshaller.unmarshal(new ByteArrayInputStream(forsendelsePayload));
            Forsendelse forsendelse = unmarshalledObject.getValue();
            return forsendelse.getForsendelsesreferanse();
        } catch (JAXBException e) {
            System.out.println("ERROR: Kunne ikke hente ut forsendelsereferanse fra forsendelse payload for fil: " + fileName);
            return null;
        }
    }


    public void addForsendelseResponse(AltinnForsendelseResponse forsendelseResponse) {
        responses.add(forsendelseResponse);
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getForsendelsePayload() {
        return forsendelsePayload;
    }

    public String getForsendelsereferanse() {
        return forsendelsereferanse;
    }

    public AltinnTrackerInformation getAltinnTrackerInformation() {
        return altinnTrackerInformation;
    }

    public ForsendelseRequestStatus getForsendelseRequestStatus() {
        return forsendelseRequestStatus;
    }

    public void setForsendelseRequestStatus(ForsendelseRequestStatus forsendelseRequestStatus) {
        this.forsendelseRequestStatus = forsendelseRequestStatus;
    }

}
