package no.kartverket.altinn.eksempelklient.service;

import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum;
import no.altinn.schemas.services.intermediary.receipt._2015._06.Receipt;
import no.altinn.schemas.services.intermediary.receipt._2015._06.ReceiptSave;
import no.altinn.schemas.services.intermediary.receipt._2015._06.ReceiptSearch;
import no.altinn.schemas.services.serviceengine.broker._2015._06.*;
import no.altinn.schemas.services.serviceengine.broker._2015._06.ObjectFactory;
import no.altinn.schemas.services.serviceentity._2015._06.BrokerServiceAvailableFileStatus;
import no.altinn.services.common.fault._2009._10.AltinnFault;
import no.altinn.services.intermediary.receipt._2009._10.IReceiptExternalBasic;
import no.altinn.services.intermediary.receipt._2009._10.IReceiptExternalBasicGetReceiptBasicV2AltinnFaultFaultFaultMessage;
import no.altinn.services.intermediary.receipt._2009._10.IReceiptExternalBasicUpdateReceiptBasicAltinnFaultFaultFaultMessage;
import no.altinn.services.serviceengine.broker._2015._06.IBrokerServiceExternalBasic;
import no.altinn.services.serviceengine.broker._2015._06.IBrokerServiceExternalBasicConfirmDownloadedBasicAltinnFaultFaultFaultMessage;
import no.altinn.services.serviceengine.broker._2015._06.IBrokerServiceExternalBasicGetAvailableFilesBasicAltinnFaultFaultFaultMessage;
import no.altinn.services.serviceengine.broker._2015._06.IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage;
import no.altinn.services.streamed.*;
import no.kartverket.altinn.eksempelklient.domain.AltinnForsendelse;
import no.kartverket.altinn.eksempelklient.domain.AltinnForsendelseResponse;
import no.kartverket.altinn.eksempelklient.domain.AltinnTrackerInformation;
import no.kartverket.altinn.eksempelklient.domain.InnsendingOperation;

import javax.xml.bind.JAXBElement;
import java.util.List;
import java.util.stream.Collectors;

public class AltinnService {

    private final AltinnServiceFactory serviceFactory;
    private final String systemUser;
    private final String systemPassword;
    private final String serviceCode;
    private final int serviceEditionCode;

    public static final String OK_RECEIPT_TEXT = "Fil lastet ned OK";

    public AltinnService(AltinnServiceFactory serviceFactory, String systemUser, String systemPassword, String serviceCode, int serviceEditionCode) {
        this.serviceFactory = serviceFactory;
        this.systemUser = systemUser;
        this.systemPassword = systemPassword;
        this.serviceCode = serviceCode;
        this.serviceEditionCode = serviceEditionCode;
    }

    public String initiateBrokerService(String sendersReference, String reportee, String recipient, InnsendingOperation operation) {
        Manifest manifest = createManifest(sendersReference, reportee, operation);
        BrokerServiceInitiation brokerServiceInitiation = createBrokerServiceInitiation(manifest, recipient);
        IBrokerServiceExternalBasic brokerServiceExternalBasicService = serviceFactory.getBrokerServiceExternalBasicService();
        try {
            return brokerServiceExternalBasicService.initiateBrokerServiceBasic(systemUser, systemPassword, brokerServiceInitiation);
        } catch (IBrokerServiceExternalBasicInitiateBrokerServiceBasicAltinnFaultFaultFaultMessage message) {
            throw new RuntimeException("ERROR: Feil ved initiering av opplasting av fil til altinn, "+ getAltinnFaultAsString(message.getFaultInfo()));
        }
    }

    public Integer uploadFileToAltinn(AltinnForsendelse altinnForsendelse, String reportee) throws IBrokerServiceExternalBasicStreamedUploadFileStreamedBasicAltinnFaultFaultFaultMessage {
        StreamedPayloadBasicBE streamedPayloadBasicBE = new StreamedPayloadBasicBE();
        streamedPayloadBasicBE.setDataStream(altinnForsendelse.pakkInnIZipFil());
        IBrokerServiceExternalBasicStreamed brokerExternalBasicServiceStreamed = serviceFactory.getBrokerServiceExternalBasicServiceStreamed();
        ReceiptExternalStreamedBE receipt = brokerExternalBasicServiceStreamed.uploadFileStreamedBasic(streamedPayloadBasicBE, altinnForsendelse.getFileName(), altinnForsendelse.getAltinnTrackerInformation().getFileReference(), reportee, systemPassword, systemUser);
        return receipt.getReceiptId();
    }

    public List<AltinnForsendelseResponse> lastNedTilgjengeligeFiler(String reportee) {
        BrokerServiceAvailableFileList avaliableFileList = hentTingjengeligeFiler(reportee);
        System.out.println(String.format("Fant %d nye filer tilgjengelig for nedlasting i Altinn", avaliableFileList.getBrokerServiceAvailableFile().size()));
        return avaliableFileList.getBrokerServiceAvailableFile().stream().map(avaliableFile -> lastNedFil(avaliableFile, reportee)).collect(Collectors.toList());
    }

    private BrokerServiceAvailableFileList hentTingjengeligeFiler(String reportee) {
        IBrokerServiceExternalBasic brokerServiceExternalBasicService = serviceFactory.getBrokerServiceExternalBasicService();
        BrokerServiceSearch searchParameters = createHentTilgjengeligeFilerSearchParam(reportee);
        try {
            return brokerServiceExternalBasicService.getAvailableFilesBasic(systemUser, systemPassword, searchParameters);
        } catch (IBrokerServiceExternalBasicGetAvailableFilesBasicAltinnFaultFaultFaultMessage message) {
            throw new RuntimeException("ERROR: Kunne ikke hente tilgjengelige filer fra altinn, "+ getAltinnFaultAsString(message.getFaultInfo()));
        }
    }

    private BrokerServiceSearch createHentTilgjengeligeFilerSearchParam(String reportee) {
        BrokerServiceSearch searchParameters = new BrokerServiceSearch();
        ObjectFactory objectFactory = new ObjectFactory();
        searchParameters.setExternalServiceCode(objectFactory.createBrokerServiceAvailableFileExternalServiceCode(serviceCode));
        searchParameters.setExternalServiceEditionCode(serviceEditionCode);
        searchParameters.setFileStatus(BrokerServiceAvailableFileStatus.UPLOADED);
        searchParameters.setReportee(reportee);
        return searchParameters;
    }


    private AltinnForsendelseResponse lastNedFil(BrokerServiceAvailableFile brokerAvaliableFile, String reportee) {
        String sendersReference = brokerAvaliableFile.getSendersReference() != null ? brokerAvaliableFile.getSendersReference().getValue() : null;
        String fileReference = brokerAvaliableFile.getFileReference();
        Integer receiptId = brokerAvaliableFile.getReceiptID();
        AltinnTrackerInformation altinnTrackerInformation = new AltinnTrackerInformation(sendersReference, fileReference, receiptId);
        AltinnForsendelseResponse forsendelseResponse = new AltinnForsendelseResponse(altinnTrackerInformation);
        IBrokerServiceExternalBasicStreamed brokerServiceExternalBasicServiceStreamed = serviceFactory.getBrokerServiceExternalBasicServiceStreamed();
        System.out.println(String.format("Lastet ned fil med filreferanse %s fra Altinn" , forsendelseResponse.getAltinnTrackerInformation().getFileReference()));
        try {
            byte[] zipPayload = brokerServiceExternalBasicServiceStreamed.downloadFileStreamedBasic(systemUser, systemPassword, brokerAvaliableFile.getFileReference(), reportee);
            forsendelseResponse.extractResponseFromZip(zipPayload);
            confirmDownload(brokerAvaliableFile.getFileReference(), reportee);
            updateReceipt(receiptId);
            System.out.println(String.format("Lastet ned zipfil som inneholder fil %s" , forsendelseResponse.getFileName()));
            return forsendelseResponse;
        } catch (IBrokerServiceExternalBasicStreamedDownloadFileStreamedBasicAltinnFaultFaultFaultMessage message) {
            throw new RuntimeException("ERROR: Kunne ikke laste ned fil fra altinn, "+getAltinnFaultAsString(message.getFaultInfo()));
        }
    }

    private void confirmDownload(String fileReference, String reportee) {
        IBrokerServiceExternalBasic brokerServiceExternalBasic = serviceFactory.getBrokerServiceExternalBasicService();
        try {
            brokerServiceExternalBasic.confirmDownloadedBasic(systemUser, systemPassword, fileReference, reportee);
        } catch (IBrokerServiceExternalBasicConfirmDownloadedBasicAltinnFaultFaultFaultMessage message) {
            throw new RuntimeException("ERROR: Kunne ikke bekrefte nedlasting av fil til Altinn: "+ getAltinnFaultAsString(message.getFaultInfo()));
        }
    }


    private BrokerServiceInitiation createBrokerServiceInitiation(Manifest manifest, String recipientPartyNumber) {
        BrokerServiceInitiation brokerServiceInitiation = new BrokerServiceInitiation();
        brokerServiceInitiation.setManifest(manifest);
        ArrayOfRecipient recipientList = new ArrayOfRecipient();
        Recipient recipient = new Recipient();
        recipient.setPartyNumber(recipientPartyNumber);
        recipientList.getRecipient().add(recipient);
        brokerServiceInitiation.setRecipientList(recipientList);
        return brokerServiceInitiation;
    }

    private Manifest createManifest(String sendersReference, String reportee, InnsendingOperation operation) {
        Manifest manifest = new Manifest();
        manifest.setExternalServiceCode(this.serviceCode);
        manifest.setExternalServiceEditionCode(this.serviceEditionCode);
        ObjectFactory objectFactory = new ObjectFactory();
        JAXBElement<ArrayOfProperty> jaxbArrayOfProperty = objectFactory.createManifestPropertyList(new ArrayOfProperty());
        Property property = new Property();
        property.setPropertyKey("operation");
        property.setPropertyValue(operation.name());
        jaxbArrayOfProperty.getValue().getProperty().add(property);
        manifest.setPropertyList(jaxbArrayOfProperty);
        manifest.setReportee(reportee);
        manifest.setSendersReference(sendersReference);
        return manifest;
    }

    public Receipt downloadReceipt(Integer receiptId) {
        IReceiptExternalBasic receiptExternalBasic = serviceFactory.getReceiptExternalBasic();
        ReceiptSearch receiptSearch = new ReceiptSearch();
        receiptSearch.setReceiptId(receiptId);
        try {
            return receiptExternalBasic.getReceiptBasicV2(systemUser, systemPassword, receiptSearch);
        } catch (IReceiptExternalBasicGetReceiptBasicV2AltinnFaultFaultFaultMessage message) {
            throw new RuntimeException(String.format("ERROR: Feil ved nedlasting av kvittering for kvitteringsid: %d, altinn fault: %s", receiptId, getAltinnFaultAsString(message.getFaultInfo())));
        }
    }

    private void updateReceipt(Integer receiptId) {
        IReceiptExternalBasic receiptExternalBasic = serviceFactory.getReceiptExternalBasic();
        ReceiptSave receipt = new ReceiptSave();
        receipt.setReceiptId(receiptId);
        receipt.setReceiptStatus(ReceiptStatusEnum.OK);
        receipt.setReceiptText(OK_RECEIPT_TEXT);
        try {
            receiptExternalBasic.updateReceiptBasic(systemUser, systemPassword, receipt);
        } catch (IReceiptExternalBasicUpdateReceiptBasicAltinnFaultFaultFaultMessage message) {
            System.out.println("ERROR: Fikk ikke oppdatert kvittering på nedlastet fil, "+getAltinnFaultAsString(message.getFaultInfo()));
        }
    }

    private String getAltinnFaultAsString(AltinnFault altinnFault) {
        if(altinnFault==null) {
            return null;
        }
        return String.format("Altinn fault: %s: %s", altinnFault.getErrorID(), altinnFault.getAltinnErrorMessage()!=null ? altinnFault.getAltinnErrorMessage().getValue() : "");
    }

    private String getAltinnFaultAsString(no.altinn.services.streamed.AltinnFault altinnFault) {
        if(altinnFault==null) {
            return null;
        }
        return String.format("Altinn fault: %s: %s", altinnFault.getErrorID(), altinnFault.getAltinnErrorMessage()!=null ? altinnFault.getAltinnErrorMessage().getValue() : "");
    }

}
