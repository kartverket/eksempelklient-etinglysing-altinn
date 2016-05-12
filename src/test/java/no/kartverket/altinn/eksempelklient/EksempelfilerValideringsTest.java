package no.kartverket.altinn.eksempelklient;

import org.junit.Test;
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
import java.util.List;

import static org.junit.Assert.assertEquals;


public class EksempelfilerValideringsTest {

    @Test
    public void sjekkAtAlleEksempelfilerValidererTest() throws IOException {
        List<String> validationFailures = new ArrayList<>();
        File[] files = new File("src/main/resources/eksempelfiler").listFiles();
        List<String> doNotValidate = Arrays.asList("ugyldig.xml");

        for (File file : files) {
            if (file.isFile() && !doNotValidate.contains(file.getName())) {
                System.out.println("Validerer " + file.getName());
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
        assertEquals(0, validationFailures.size());
    }


    private boolean validate(File requestFile) throws SAXException, IOException {
        Source schemaSource= new StreamSource(getClass().getResourceAsStream("/innsending.xsd"));

        Source xmlFile = new StreamSource(requestFile);
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(schemaSource);
        Validator validator = schema.newValidator();
        try {
            validator.validate(xmlFile);
            System.out.println(xmlFile.getSystemId() + " is valid");
            return true;
        } catch (SAXException e) {
            System.out.println(xmlFile.getSystemId() + " is NOT valid");
            System.out.println("Reason: " + e.getLocalizedMessage());
            return false;
        }
    }
}
