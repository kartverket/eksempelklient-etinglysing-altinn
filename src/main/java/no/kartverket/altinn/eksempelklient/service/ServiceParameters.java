package no.kartverket.altinn.eksempelklient.service;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ServiceParameters {
    private Properties environment;

    public ServiceParameters(Properties environment) {
        this.environment = environment;
        validate();
    }

    public String getSeverAddress() {
        return (String) environment.get(ParameterKey.Server.getKey());

    }

    public String getServiceCode() {
        return (String) environment.get(ParameterKey.ServiceCode.getKey());
    }

    public int  getServiceEdtionCode() {
        try {
            return Integer.parseInt((String) environment.get(ParameterKey.ServiceEditionCode.getKey()));
        }
        catch (NumberFormatException e) {
            throw new RuntimeException(String.format("Feil i konfigurasjon: %s må være en numerisk verdi", ParameterKey.ServiceEditionCode.getKey()));
        }
    }
    public String getSystemUserName() {
        return (String) environment.get(ParameterKey.SystemUserName.getKey());
    }

    public String getSystemPassword() {
        return (String) environment.get(ParameterKey.SystemPassword.getKey());
    }

    public String getReportee() {
        return (String) environment.get(ParameterKey.Reportee.getKey());
    }

    public String getRecepient() {
        return (String) environment.get(ParameterKey.Recepient.getKey());
    }

    private void validate() {
        List<String> configurationErrors = new ArrayList<>();
        for (ParameterKey parameterKey : ParameterKey.values()) {
            if (!environment.containsKey(parameterKey.getKey())) {
                configurationErrors.add(String.format("manglende verdi for konfigurasjonsparameter '%s'", parameterKey.getKey()));
            }
        }
        if (!configurationErrors.isEmpty()) {
            throw new RuntimeException("Feil i konfigurasjon:"+ Joiner.on("\n").join(configurationErrors));
        }

    }

    public enum  ParameterKey {
        Server("altinn.server"),
        ServiceCode("altinn.serviceCode"),
        ServiceEditionCode("altinn.serviceEditionCode"),
        SystemUserName("altinn.user"),
        SystemPassword("altinn.password"),
        Reportee("altinn.reportee"),
        Recepient("altinn.recepient");

        private String key;

        ParameterKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }
}
