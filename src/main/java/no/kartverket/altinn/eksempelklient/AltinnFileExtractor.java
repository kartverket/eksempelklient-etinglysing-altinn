package no.kartverket.altinn.eksempelklient;

import com.google.common.io.ByteStreams;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class AltinnFileExtractor {
    private final String name;
    private byte[] target = new byte[0];
    private byte[] manifestTarget = new byte[0];

    public String getName() {
        return name;
    }

    public AltinnFileExtractor(File content) {
        checkNotNull(content);
        List<String> files = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(content)) {
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                final ByteArrayOutputStream data = new ByteArrayOutputStream();
                final ZipEntry entry = entries.nextElement();
                try (final InputStream inputStream = zipFile.getInputStream(entry)) {
                    ByteStreams.copy(inputStream, data);
                    if (entry.getName().equals("manifest.xml")) {
                        manifestTarget = data.toByteArray();
                    } else {
                        files.add(entry.getName());
                        target = data.toByteArray();
                    }
                }
            }
            checkState(files.size() == 1, "Multiple files in zip is not supported", files);
            name = files.get(0);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public byte[] getTargetRaw() {
        return target;
    }

    public byte[] getManifestTargetRaw() {
        return manifestTarget;
    }
}
