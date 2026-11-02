package com.waterborne.repository;

import com.waterborne.model.DiseaseRecord;
import com.waterborne.util.CsvResourceReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DiseaseRepository {
    private static final String RESOURCE = "/data/water_pollution_disease.csv";
    private static volatile List<DiseaseRecord> cachedRecords;

    public List<DiseaseRecord> findAll() throws IOException {
        List<DiseaseRecord> records = cachedRecords;
        if (records == null) {
            synchronized (DiseaseRepository.class) {
                records = cachedRecords;
                if (records == null) {
                    records = load();
                    cachedRecords = records;
                }
            }
        }
        return records;
    }

    private List<DiseaseRecord> load() throws IOException {
        List<List<String>> rows = CsvResourceReader.read(RESOURCE);
        Map<String, Integer> columns = indexColumns(rows.get(0));
        List<DiseaseRecord> records = new ArrayList<>();

        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            records.add(new DiseaseRecord(
                    text(value(row, columns, "country")),
                    text(value(row, columns, "region")),
                    integer(value(row, columns, "year")),
                    text(value(row, columns, "watersourcetype")),
                    number(value(row, columns, "contaminantlevelppm")),
                    number(value(row, columns, "accesstocleanwaterofpopulation")),
                    number(value(row, columns, "sanitationcoverageofpopulation")),
                    number(value(row, columns, "diarrhealcasesper100000people")),
                    number(value(row, columns, "choleracasesper100000people")),
                    number(value(row, columns, "typhoidcasesper100000people"))));
        }
        if (records.isEmpty()) {
            throw new IOException("Disease and environmental CSV contains no records.");
        }
        return Collections.unmodifiableList(records);
    }

    private Map<String, Integer> indexColumns(List<String> headers) throws IOException {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            columns.put(normalize(headers.get(i)), i);
        }
        String[] required = {
                "country", "region", "year", "watersourcetype", "contaminantlevelppm",
                "accesstocleanwaterofpopulation",
                "sanitationcoverageofpopulation", "diarrhealcasesper100000people",
                "choleracasesper100000people", "typhoidcasesper100000people"
        };
        for (String column : required) {
            if (!columns.containsKey(column)) {
                throw new IOException("Disease and environmental CSV is missing required column: " + column);
            }
        }
        return columns;
    }

    private String value(List<String> row, Map<String, Integer> columns, String column) {
        int index = columns.get(column);
        return index < row.size() ? row.get(index) : "";
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private String text(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "Unknown" : trimmed;
    }

    private Integer integer(String value) {
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double number(String value) {
        try {
            double parsed = Double.parseDouble(value.trim());
            return Double.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
