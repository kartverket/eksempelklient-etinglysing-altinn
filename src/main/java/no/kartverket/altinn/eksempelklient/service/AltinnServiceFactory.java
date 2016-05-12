package no.kartverket.altinn.eksempelklient.service;

import no.altinn.services.intermediary.receipt._2009._10.IReceiptExternalBasic;
import no.altinn.services.intermediary.receipt._2009._10.ReceiptExternalBasicSF;
import no.altinn.services.serviceengine.broker._2015._06.BrokerServiceExternalBasicSF;
import no.altinn.services.serviceengine.broker._2015._06.IBrokerServiceExternalBasic;
import no.altinn.services.streamed.BrokerServiceExternalBasicStreamedSF;
import no.altinn.services.streamed.IBrokerServiceExternalBasicStreamed;

import javax.xml.ws.BindingProvider;

public class AltinnServiceFactory {

    private String serverAddress;

    public AltinnServiceFactory(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public IBrokerServiceExternalBasic getBrokerServiceExternalBasicService() {
        BrokerServiceExternalBasicSF webServiceClient = new BrokerServiceExternalBasicSF();
        IBrokerServiceExternalBasic port = webServiceClient.getPort(IBrokerServiceExternalBasic.class);
        final String endpointAddress = serverAddress + "/ServiceEngineExternal/BrokerServiceExternalBasic.svc";
        setServerAddress(port, endpointAddress);
        return port;
    }

    public IBrokerServiceExternalBasicStreamed getBrokerServiceExternalBasicServiceStreamed() {
        BrokerServiceExternalBasicStreamedSF webServiceClient = new BrokerServiceExternalBasicStreamedSF();
        IBrokerServiceExternalBasicStreamed port = webServiceClient.getPort(IBrokerServiceExternalBasicStreamed.class);
        final String endpointAddress = serverAddress + "/ServiceEngineExternal/BrokerServiceExternalBasicStreamed.svc";
        setServerAddress(port, endpointAddress);
        return port;
    }

    public IReceiptExternalBasic getReceiptExternalBasic() {
        ReceiptExternalBasicSF webServiceClient = new ReceiptExternalBasicSF();
        IReceiptExternalBasic port = webServiceClient.getPort(IReceiptExternalBasic.class);
        final String endpointAddress = serverAddress + "/IntermediaryExternal/ReceiptExternalBasic.svc";
        setServerAddress(port, endpointAddress);
        return port;
    }


    private <P> void setServerAddress(P port, String endpointAddress) {
        BindingProvider bindingProvider = (BindingProvider) port;
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
    }
}
