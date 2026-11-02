package com.waterborne.service;

import com.waterborne.model.WaterQualityRecord;
import com.waterborne.repository.WaterQualityRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class WaterQualityService {
    private final List<WaterQualityRecord> records;

    public WaterQualityService() throws IOException {
        records = new WaterQualityRepository().findAll();
    }

    public List<WaterQualityRecord> getAllRecords() {
        return records;
    }

    public List<WaterQualityRecord> filter(String state, String waterBodyType, String search) {
        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return records.stream()
                .filter(record -> matches(state, record.state()))
                .filter(record -> matches(waterBodyType, record.waterBodyType()))
                .filter(record -> query.isEmpty()
                        || record.stationCode().toLowerCase(Locale.ROOT).contains(query)
                        || record.location().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    public long highColiformCount(List<WaterQualityRecord> filtered) {
        double fecalThreshold = percentile(records.stream()
                .map(WaterQualityRecord::averageFecalColiform)
                .filter(Objects::nonNull).toList(), 0.75);
        double totalThreshold = percentile(records.stream()
                .map(WaterQualityRecord::averageTotalColiform)
                .filter(Objects::nonNull).toList(), 0.75);
        return filtered.stream().filter(record ->
                (record.averageFecalColiform() != null
                        && record.averageFecalColiform() >= fecalThreshold)
                        || (record.averageTotalColiform() != null
                        && record.averageTotalColiform() >= totalThreshold)).count();
    }

    public static double average(List<WaterQualityRecord> records,
                                 Function<WaterQualityRecord, Double> measure) {
        return records.stream().map(measure).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    public static double percentile(List<Double> values, double percentile) {
        if (values.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Comparator.naturalOrder());
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    private boolean matches(String filter, String value) {
        return filter == null || "All".equals(filter) || filter.equals(value);
    }
}
