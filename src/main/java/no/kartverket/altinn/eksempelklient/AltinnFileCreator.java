package no.kartverket.altinn.eksempelklient;

import com.google.common.io.ByteStreams;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AltinnFileCreator {

    public AltinnFileCreator(String filename, byte[] content, OutputStream outputStream) {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, Charset.forName("ISO-8859-1"))){
            zipOutputStream.setMethod(ZipOutputStream.STORED);
            createFileEntry(filename, content, zipOutputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createFileEntry(String filename, byte[] content, ZipOutputStream zipOutputStream) throws IOException {
        ZipEntry response = new ZipEntry(filename);
        CRC32 crc32 = new CRC32();
        crc32.update(content);
        response.setCrc(crc32.getValue());
        response.setSize(content.length);
        response.setCompressedSize(content.length);
        zipOutputStream.putNextEntry(response);
        ByteStreams.copy(new ByteArrayInputStream(content), zipOutputStream);
        zipOutputStream.closeEntry();
    }
}
