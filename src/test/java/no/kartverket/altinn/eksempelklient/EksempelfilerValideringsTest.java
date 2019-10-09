package no.kartverket.altinn.eksempelklient;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class EksempelfilerValideringsTest {

    private final Logger log = LoggerFactory.getLogger(EksempelfilerValideringsTest.class);

    @Test
    public void sjekkAtAlleEksempelfilerValidererTest() throws IOException {
        List<String> validationFailures = new ArrayList<>();
        File[] files = new File("src/main/resources/eksempelfiler").listFiles();
        List<String> doNotValidate = Collections.singletonList("ugyldig.xml");

        assert files != null;

        for (File file : files) {
            if (file.isFile() && !doNotValidate.contains(file.getName())) {
                log.info("Validerer {}", file.getName());
                try {
                    boolean validationOk = validate(file);
                    if(!validationOk) {
                        validationFailures.add(file.getName());
                    }
                } catch (SAXException e) {
                    e.printStackTrace();
                    validationFailures.add(file.getName());
                }
            }
        }
        assertEquals(listFilesWithValidationFailures(validationFailures), 0, validationFailures.size());
    }

    private String listFilesWithValidationFailures(List<String> validationFailures) {
        StringBuilder result = new StringBuilder();
        validationFailures.forEach(failure -> result.append(failure).append(", "));
        return result.toString();
    }


    private boolean validate(File requestFile) throws SAXException, IOException {
        Source schemaSource= new StreamSource(getClass().getResourceAsStream("/innsending.xsd"));
        Source xmlFile = new StreamSource(requestFile);
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(schemaSource);
        Validator validator = schema.newValidator();

        try {
            validator.validate(xmlFile);
            log.info("{} is valid", xmlFile.getSystemId());
            return true;
        } catch (SAXException e) {
            log.error("{} is NOT valid", xmlFile.getSystemId());
            log.error("Reason: {}", e.getLocalizedMessage());
            return false;
        }
    }
}
