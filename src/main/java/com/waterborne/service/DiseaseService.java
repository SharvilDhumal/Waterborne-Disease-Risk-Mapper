package com.waterborne.service;

import com.waterborne.model.DiseaseRecord;
import com.waterborne.repository.DiseaseRepository;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class DiseaseService {
    private final List<DiseaseRecord> records;

    public DiseaseService() throws IOException {
        records = new DiseaseRepository().findAll();
    }

    public List<DiseaseRecord> getAllRecords() {
        return records;
    }

    public List<DiseaseRecord> filter(String country, String region, String year, String waterSourceType) {
        return records.stream()
                .filter(record -> matches(country, record.country()))
                .filter(record -> matches(region, record.region()))
                .filter(record -> matches(year, record.year() == null ? "" : record.year().toString()))
                .filter(record -> matches(waterSourceType, record.waterSourceType()))
                .toList();
    }

    public static double average(List<DiseaseRecord> records, java.util.function.Function<DiseaseRecord, Double> measure) {
        return records.stream().map(measure).filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    private boolean matches(String filter, String value) {
        return filter == null || "All".equals(filter) || filter.toLowerCase(Locale.ROOT).equals(
                value.toLowerCase(Locale.ROOT));
    }
}
