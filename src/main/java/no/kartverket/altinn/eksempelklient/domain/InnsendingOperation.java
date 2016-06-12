package no.kartverket.altinn.eksempelklient.domain;

public enum InnsendingOperation {
    sendTilTinglysing, valider, hentStatus;

    @Override
    public String toString() {
        return name();
    }
}
