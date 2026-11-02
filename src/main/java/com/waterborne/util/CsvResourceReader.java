package com.waterborne.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class CsvResourceReader {
    private CsvResourceReader() {
    }

    public static List<List<String>> read(String resourcePath) throws IOException {
        InputStream stream = CsvResourceReader.class.getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IOException("CSV resource was not found: " + resourcePath);
        }

        byte[] bytes;
        try (stream) {
            bytes = stream.readAllBytes();
        }

        Charset charset;
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .decode(java.nio.ByteBuffer.wrap(bytes));
            charset = StandardCharsets.UTF_8;
        } catch (CharacterCodingException e) {
            charset = Charset.forName("windows-1252");
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(bytes), charset))) {
            return parse(reader);
        }
    }

    private static List<List<String>> parse(BufferedReader reader) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        int current;
        boolean firstCharacter = true;

        while ((current = reader.read()) != -1) {
            char character = (char) current;
            if (firstCharacter) {
                firstCharacter = false;
                if (character == '\uFEFF') {
                    continue;
                }
            }

            if (quoted) {
                if (character == '"') {
                    reader.mark(1);
                    int next = reader.read();
                    if (next == '"') {
                        field.append('"');
                    } else {
                        quoted = false;
                        if (next != -1) {
                            reader.reset();
                        }
                    }
                } else {
                    field.append(character);
                }
            } else if (character == '"' && field.isEmpty()) {
                quoted = true;
            } else if (character == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else if (character == '\n' || character == '\r') {
                if (character == '\r') {
                    reader.mark(1);
                    int next = reader.read();
                    if (next != '\n' && next != -1) {
                        reader.reset();
                    }
                }
                row.add(field.toString());
                field.setLength(0);
                if (!(row.size() == 1 && row.get(0).isBlank())) {
                    rows.add(row);
                }
                row = new ArrayList<>();
            } else {
                field.append(character);
            }
        }

        if (quoted) {
            throw new IOException("CSV ended inside a quoted field.");
        }
        if (!row.isEmpty() || !field.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        if (rows.isEmpty()) {
            throw new IOException("CSV resource is empty.");
        }
        return rows;
    }
}
