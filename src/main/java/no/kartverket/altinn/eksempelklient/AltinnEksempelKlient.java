package no.kartverket.altinn.eksempelklient;

import no.kartverket.altinn.eksempelklient.domain.AltinnForsendelse;
import no.kartverket.altinn.eksempelklient.domain.InnsendingOperation;
import no.kartverket.altinn.eksempelklient.service.AltinnService;
import no.kartverket.altinn.eksempelklient.service.AltinnServiceFactory;
import no.kartverket.altinn.eksempelklient.service.ServiceParameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AltinnEksempelKlient {


    public static final int POLL_INTERVAL_MILLIS = 10000;

    /**
     * Konfigureres via
     * {@code -Daltinn.server=https://tt02.altinn.no -Daltinn.serviceCode=4433 -Daltinn.serviceEditionCode=xx -Daltinn.user=xxxx -Daltinn.password=xxxx -Daltinn.reportee=123456789 -Daltinn.recepient=910976168 }
     *
     * altinn.server = angir server adresse for altinn formidlingstjenesten, for test benyttes https://tt02.altinn.no
     * altinn.serviceCode/altinn.serviceEditionCode refererer til hvilken utgave av Kartverket sin tjeneste man skal gå mot
     * altinn.user/altinn.password må settes opp av sluttbruker selv i Altinn sluttbruker løsning for formidlingstjenesten
     * altinn.reportee - organisasjonsnummer for innsender, for test får man tildelt et fiktivt organisasjonsnummer
     * altinn.recepient - organsisasjonsnummer for Kartverket, for test blir det benyttet et fiktivt organisasjonsnummer på samme måte som for innender: 910976168. For produksjon skal det være: 971040238
     *
     * * @param args filnavn på dokument som skal tinglyses.
     */
    public static void main(String[] args) {
        ServiceParameters parameters = new ServiceParameters(System.getProperties());

        AltinnServiceFactory serviceFactory = new AltinnServiceFactory(parameters.getSeverAddress());
        AltinnService altinnService = new AltinnService(serviceFactory, parameters.getSystemUserName(), parameters.getSystemPassword(), parameters.getServiceCode(), parameters.getServiceEdtionCode());


        if(args.length < 2) {
            System.err.println("Må angi operasjon og minst en inputfil som argument");
            System.exit(-1);
        }

        InnsendingOperation operation = getOperation(args[0]);

        //Scenario 1: Last opp filer til Altinn
        List<AltinnForsendelse> altinnForsendelser = new ArrayList<>();
        for (int i=1; i<args.length; i++) {
            String inputfil = args[i];
            altinnForsendelser.add(new AltinnForsendelse(inputfil, "Ref"+inputfil, operation));
        }
        LastOppMeldingScenario lastOppMeldingScenario = new LastOppMeldingScenario(altinnService, parameters.getReportee(), parameters.getRecepient());
        altinnForsendelser.stream().forEach(lastOppMeldingScenario::execute);

        //Scenario 2: Hent kvitteringer for innsendte filer
        VentPaaKvitteringFraMottakerScenario ventPaaKvitteringFraMottakerScenario = new VentPaaKvitteringFraMottakerScenario(altinnService);
        altinnForsendelser.stream().forEach(ventPaaKvitteringFraMottakerScenario::execute);


        //Scenario 3: Last ned fil fra Altinn hvis innsending gikk ok
        LastNedTilgjengeligeFilerScenario lastNedTilgjengeligeFilerScenario = new LastNedTilgjengeligeFilerScenario(altinnService, parameters.getReportee());

        //Lar denne stå å lytte på innsendte filer til vi stopper klienten.
        while(true) {
            lastNedTilgjengeligeFilerScenario.execute(altinnForsendelser);
            System.out.println(String.format("Venter '%d' milisekunder før neste forespørsel mot Altinn", POLL_INTERVAL_MILLIS));
            try {
                Thread.sleep(POLL_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static InnsendingOperation getOperation(String operationAsString) {
        try {
            return InnsendingOperation.valueOf(operationAsString);
        }
        catch (IllegalArgumentException e) {
            List<String> validOperations = Arrays.asList(InnsendingOperation.values()).stream().map(Enum::name).collect(Collectors.toList());
            System.err.println(String.format("Første parameter må angi operasjon (Gyldige verdier: %s). Angitt verdi er ugyldig: '%s'", String.join(" ,", validOperations), operationAsString));
            System.exit(-1);
            return null;
        }
    }

}
