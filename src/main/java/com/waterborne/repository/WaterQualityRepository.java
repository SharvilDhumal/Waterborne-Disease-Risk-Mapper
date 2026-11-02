package com.waterborne.repository;

import com.waterborne.model.WaterQualityRecord;
import com.waterborne.util.CsvResourceReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WaterQualityRepository {
    private static final String RESOURCE = "/data/Water_pond_tanks_2021.csv";
    private static volatile List<WaterQualityRecord> cachedRecords;

    public List<WaterQualityRecord> findAll() throws IOException {
        List<WaterQualityRecord> records = cachedRecords;
        if (records == null) {
            synchronized (WaterQualityRepository.class) {
                records = cachedRecords;
                if (records == null) {
                    records = load();
                    cachedRecords = records;
                }
            }
        }
        return records;
    }

    private List<WaterQualityRecord> load() throws IOException {
        List<List<String>> rows = CsvResourceReader.read(RESOURCE);
        Map<String, Integer> columns = indexColumns(rows.get(0));
        List<WaterQualityRecord> records = new ArrayList<>();

        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            records.add(new WaterQualityRecord(
                    text(value(row, columns, "stncode")),
                    text(value(row, columns, "nameofmonitoringlocation")),
                    text(value(row, columns, "typewaterbody")),
                    text(value(row, columns, "statename")),
                    number(value(row, columns, "temperaturecmin")),
                    number(value(row, columns, "temperaturecmax")),
                    number(value(row, columns, "dissolvedoxygenmglmin")),
                    number(value(row, columns, "dissolvedoxygenmglmax")),
                    number(value(row, columns, "phmin")),
                    number(value(row, columns, "phmax")),
                    number(value(row, columns, "conductivitymhoscmmin")),
                    number(value(row, columns, "conductivitymhoscmmax")),
                    number(value(row, columns, "bodmglmin")),
                    number(value(row, columns, "bodmglmax")),
                    number(value(row, columns, "nitratennitritenmglmin")),
                    number(value(row, columns, "nitratennitritenmglmax")),
                    number(value(row, columns, "fecalcoliformmpn100mlmin")),
                    number(value(row, columns, "fecalcoliformmpn100mlmax")),
                    number(value(row, columns, "totalcoliformmpn100mlmin")),
                    number(value(row, columns, "totalcoliformmpn100mlmax"))));
        }

        if (records.isEmpty()) {
            throw new IOException("Water-quality CSV contains no monitoring records.");
        }
        return Collections.unmodifiableList(records);
    }

    private Map<String, Integer> indexColumns(List<String> headers) throws IOException {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            columns.put(normalize(headers.get(i)), i);
        }

        String[] required = {
                "stncode", "nameofmonitoringlocation", "typewaterbody", "statename",
                "temperaturecmin", "temperaturecmax", "dissolvedoxygenmglmin",
                "dissolvedoxygenmglmax", "phmin", "phmax", "conductivitymhoscmmin",
                "conductivitymhoscmmax", "bodmglmin", "bodmglmax", "nitratennitritenmglmin",
                "nitratennitritenmglmax", "fecalcoliformmpn100mlmin",
                "fecalcoliformmpn100mlmax", "totalcoliformmpn100mlmin",
                "totalcoliformmpn100mlmax"
        };
        for (String column : required) {
            if (!columns.containsKey(column)) {
                throw new IOException("Water-quality CSV is missing required column: " + column);
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

    private Double number(String value) {
        try {
            double parsed = Double.parseDouble(value.trim());
            return Double.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
