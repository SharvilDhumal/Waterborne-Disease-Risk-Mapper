package com.waterborne.repository;

import com.waterborne.algorithm.RiskCalculatorServiceImpl;
import com.waterborne.model.District;
import com.waterborne.model.RiskLevel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only repository backed by the bundled Census 2011 CSV dataset.
 */
public class CensusCsvDistrictRepository implements DistrictRepository {

    private static final String DATASET_RESOURCE = "/data/india-districts-census-2011.csv";
    private final RiskCalculatorServiceImpl riskCalculator = new RiskCalculatorServiceImpl();

    @Override
    public List<District> findAll() throws IOException {
        InputStream dataset = getClass().getResourceAsStream(DATASET_RESOURCE);
        if (dataset == null) {
            throw new IOException("Bundled census dataset was not found: " + DATASET_RESOURCE);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(dataset, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("Bundled census dataset is empty.");
            }

            Map<String, Integer> columns = indexColumns(parseCsvLine(headerLine));
            List<District> districts = new ArrayList<>();
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                List<String> values = parseCsvLine(line);
                try {
                    districts.add(createDistrict(columns, values));
                } catch (IllegalArgumentException e) {
                    throw new IOException("Invalid census data at CSV line " + lineNumber + ": "
                            + e.getMessage(), e);
                }
            }

            if (districts.isEmpty()) {
                throw new IOException("Bundled census dataset contains no district records.");
            }
            return districts;
        }
    }

    private District createDistrict(Map<String, Integer> columns, List<String> values) {
        int id = parseInteger(value(columns, values, "District code"), "District code");
        String name = value(columns, values, "District name").trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("District name is empty.");
        }

        long population = parseLong(value(columns, values, "Population"), "Population");
        long households = Math.max(1, parseLong(value(columns, values, "Households"), "Households"));
        long latrineHouseholds = parseLong(value(columns, values,
                "Having_latrine_facility_within_the_premises_Total_Households"),
                "Having_latrine_facility_within_the_premises_Total_Households");
        long tapWaterHouseholds = parseLong(value(columns, values,
                "Main_source_of_drinking_water_Tapwater_Households"),
                "Main_source_of_drinking_water_Tapwater_Households");
        long boreholeHouseholds = parseLong(value(columns, values,
                "Main_source_of_drinking_water_Tubewell_Borehole_Households"),
                "Main_source_of_drinking_water_Tubewell_Borehole_Households");

        double sanitationCoverage = clamp(round((latrineHouseholds / (double) households) * 100.0, 2), 0.0, 100.0);
        double waterScarcity = clamp(round(
                100.0 - ((tapWaterHouseholds + boreholeHouseholds) / (double) households) * 100.0, 2),
                0.0, 100.0);
        double populationDensity = round(population / (households * 0.05), 1);
        double riskScore = riskCalculator.calculateRiskScore(
                waterScarcity, sanitationCoverage, population, populationDensity);

        District district = new District(id, name, waterScarcity, sanitationCoverage, population, populationDensity,
                riskScore, riskCalculator.classifyRisk(riskScore));
        district.setState(value(columns, values, "State name"));
        district.setCensusMetrics(waterScarcity, sanitationCoverage);
        return district;
    }

    private Map<String, Integer> indexColumns(List<String> headers) throws IOException {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            if (i == 0) {
                header = header.replace("\uFEFF", "");
            }
            columns.put(header.trim(), i);
        }

        String[] requiredColumns = {
                "District code",
                "State name",
                "District name",
                "Population",
                "Households",
                "Having_latrine_facility_within_the_premises_Total_Households",
                "Main_source_of_drinking_water_Tapwater_Households",
                "Main_source_of_drinking_water_Tubewell_Borehole_Households"
        };
        for (String column : requiredColumns) {
            if (!columns.containsKey(column)) {
                throw new IOException("Census dataset is missing required column: " + column);
            }
        }
        return columns;
    }

    private String value(Map<String, Integer> columns, List<String> values, String column) {
        int index = columns.get(column);
        if (index >= values.size()) {
            throw new IllegalArgumentException("Missing value for " + column + ".");
        }
        return values.get(index).trim();
    }

    private int parseInteger(String value, String column) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer in " + column + ": " + value, e);
        }
    }

    private long parseLong(String value, String column) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number in " + column + ": " + value, e);
        }
    }

    private double round(double value, int decimalPlaces) {
        double scale = Math.pow(10, decimalPlaces);
        return Math.round(value * scale) / scale;
    }

    private double clamp(double value, double minimum, double maximum) {
        return Math.min(maximum, Math.max(minimum, value));
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (current == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }
        values.add(value.toString());
        return values;
    }

    @Override
    public Optional<District> findByName(String name) throws IOException {
        return findAll().stream()
                .filter(district -> district.getName().equalsIgnoreCase(name == null ? "" : name.trim()))
                .findFirst();
    }

    @Override
    public boolean save(District district) {
        throw new UnsupportedOperationException("The bundled census dataset is read-only.");
    }

    @Override
    public boolean update(District district) {
        throw new UnsupportedOperationException("The bundled census dataset is read-only.");
    }

    @Override
    public boolean deleteById(int id) {
        throw new UnsupportedOperationException("The bundled census dataset is read-only.");
    }
}
