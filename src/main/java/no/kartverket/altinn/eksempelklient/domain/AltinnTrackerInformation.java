package no.kartverket.altinn.eksempelklient.domain;

public class AltinnTrackerInformation {

    private String sendersReference;
    private String fileReference;
    private Integer receiptId;

    public AltinnTrackerInformation() {
    }

    public AltinnTrackerInformation(String sendersReference, String fileReference, Integer receiptId) {
        this.sendersReference = sendersReference;
        this.fileReference = fileReference;
        this.receiptId = receiptId;
    }

    public String getSendersReference() {
        return sendersReference;
    }

    public void setSendersReference(String sendersReference) {
        this.sendersReference = sendersReference;
    }

    public String getFileReference() {
        return fileReference;
    }

    public void setFileReference(String fileReference) {
        this.fileReference = fileReference;
    }

    public Integer getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(Integer receiptId) {
        this.receiptId = receiptId;
    }

    public boolean harSammeSendersReference(AltinnTrackerInformation altinnTrackerInformation) {
        return sendersReference != null && sendersReference.equals(altinnTrackerInformation.getSendersReference());
    }
}
