package org.oddlama.vane.core.resourcepack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONObject;

public class ResourcePackGenerator {
    private Map<String, Map<String, JSONObject>> translations = new HashMap<>();
    private Map<String, byte[]> files = new HashMap<>();

    public JSONObject translations(String namespace, String langCode) {
        var ns = translations.computeIfAbsent(namespace, k -> new HashMap<>());
        var langMap = ns.get(langCode);
        if (langMap == null) {
            langMap = new JSONObject();
            ns.put(langCode, langMap);
        }
        return langMap;
    }

    private void writeTranslations(final ZipOutputStream zip) throws IOException {
        for (var t : translations.entrySet()) {
            var namespace = t.getKey();
            for (var ns : t.getValue().entrySet()) {
                var langCode = ns.getKey();
                var langMap = ns.getValue();
                zip.putNextEntry(new ZipEntry("assets/" + namespace + "/lang/" + langCode + ".json"));
                zip.write(langMap.toString().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }

	public void addFile(String path, InputStream stream) throws IOException {
		files.put(path, stream.readAllBytes());
	}

    public void write(File file) throws IOException {
        try (var zip = new ZipOutputStream(new FileOutputStream(file))) {
            writeTranslations(zip);

			// Add all files
            for (var f : files.entrySet()) {
                var path = f.getKey();
                var content = f.getValue();
                zip.putNextEntry(new ZipEntry(path));
                zip.write(content);
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw e;
        }
    }
}
