package com.waterborne.service;

import com.waterborne.model.District;
import com.waterborne.model.RiskLevel;
import com.waterborne.model.RiskStatistics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Mock Data Implementation of DistrictService for UI design and testing.
 * Provides realistic Maharashtra district data without requiring MySQL active connection.
 */
public class MockDistrictDataService implements DistrictService {

    private final List<District> mockDistricts;

    public MockDistrictDataService() {
        mockDistricts = new ArrayList<>();
        // Mock dataset with real district names & realistic environmental metrics
        mockDistricts.add(new District(1, "Pune", 72.0, 45.0, 3200000L, 1600.0));
        mockDistricts.add(new District(2, "Nashik", 63.0, 52.0, 2100000L, 950.0));
        mockDistricts.add(new District(3, "Thane", 38.0, 78.0, 1800000L, 1200.0));
        mockDistricts.add(new District(4, "Nagpur", 68.0, 48.0, 2400000L, 1100.0));
        mockDistricts.add(new District(5, "Mumbai City", 25.0, 85.0, 3100000L, 4500.0));
        mockDistricts.add(new District(6, "Satara", 45.0, 62.0, 1400000L, 550.0));
        mockDistricts.add(new District(7, "Solapur", 82.0, 38.0, 1900000L, 780.0));
        mockDistricts.add(new District(8, "Aurangabad", 76.0, 42.0, 2200000L, 1050.0));
        mockDistricts.add(new District(9, "Kolhapur", 32.0, 81.0, 1600000L, 720.0));
        mockDistricts.add(new District(10, "Ahmednagar", 79.0, 40.0, 2600000L, 640.0));
        mockDistricts.add(new District(11, "Latur", 88.0, 32.0, 1300000L, 610.0));
        mockDistricts.add(new District(12, "Raigad", 29.0, 79.0, 1100000L, 430.0));
    }

    @Override
    public List<District> getAllDistricts() {
        return new ArrayList<>(mockDistricts);
    }

    @Override
    public Optional<District> getDistrictByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return mockDistricts.stream()
                .filter(d -> d.getName().equalsIgnoreCase(name.trim()))
                .findFirst();
    }

    @Override
    public List<District> filterDistricts(String searchQuery, RiskLevel riskLevelFilter, String sortByOption) {
        return mockDistricts.stream()
                .filter(d -> {
                    if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                        return d.getName().toLowerCase().contains(searchQuery.trim().toLowerCase());
                    }
                    return true;
                })
                .filter(d -> {
                    if (riskLevelFilter != null) {
                        return d.getRiskLevel() == riskLevelFilter;
                    }
                    return true;
                })
                .sorted((d1, d2) -> {
                    if (sortByOption == null) return 0;
                    switch (sortByOption) {
                        case "Risk Score — High to Low":
                            return Double.compare(d2.getRiskScore(), d1.getRiskScore());
                        case "Risk Score — Low to High":
                            return Double.compare(d1.getRiskScore(), d2.getRiskScore());
                        case "District Name — A-Z":
                            return d1.getName().compareToIgnoreCase(d2.getName());
                        case "Water Scarcity — High to Low":
                            return Double.compare(d2.getWaterScarcity(), d1.getWaterScarcity());
                        case "Sanitation — Low to High":
                            return Double.compare(d1.getSanitationCoverage(), d2.getSanitationCoverage());
                        case "Population — High to Low":
                            return Long.compare(d2.getPopulation(), d1.getPopulation());
                        default:
                            return 0;
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public RiskStatistics getRiskStatistics() {
        return new RiskStatistics(mockDistricts);
    }

    @Override
    public List<District> getRiskRanking() {
        return mockDistricts.stream()
                .sorted(Comparator.comparingDouble(District::getRiskScore).reversed())
                .collect(Collectors.toList());
    }
}
