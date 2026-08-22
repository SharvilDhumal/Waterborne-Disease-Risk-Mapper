package com.waterborne.algorithm;

import com.waterborne.model.District;
import com.waterborne.model.RiskLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom DSA Implementation of RiskCalculatorService for Student 1.
 * Features weighted mathematical risk scoring and custom QuickSort algorithms.
 */
public class RiskCalculatorServiceImpl implements RiskCalculatorService {

    @Override
    public double calculateRiskScore(double waterScarcity, double sanitationCoverage, long population, double populationDensity) {
        double densityIndex = Math.min(100.0, (populationDensity / 1500.0) * 100.0);
        // Risk Score Formula: 45% Scarcity + 35% Deficit Sanitation + 20% Density
        double rawScore = 0.45 * waterScarcity + 0.35 * (100.0 - sanitationCoverage) + 0.20 * densityIndex;
        return Math.round(rawScore * 10.0) / 10.0;
    }

    @Override
    public RiskLevel classifyRisk(double riskScore) {
        return RiskLevel.fromScore(riskScore);
    }

    @Override
    public List<District> sortByRiskScore(List<District> districts, boolean ascending) {
        if (districts == null || districts.size() <= 1) return districts;
        List<District> copy = new ArrayList<>(districts);
        quickSortByScore(copy, 0, copy.size() - 1, ascending);
        return copy;
    }

    private void quickSortByScore(List<District> list, int low, int high, boolean ascending) {
        if (low < high) {
            int pi = partitionByScore(list, low, high, ascending);
            quickSortByScore(list, low, pi - 1, ascending);
            quickSortByScore(list, pi + 1, high, ascending);
        }
    }

    private int partitionByScore(List<District> list, int low, int high, boolean ascending) {
        double pivot = list.get(high).getRiskScore();
        int i = (low - 1);
        for (int j = low; j < high; j++) {
            boolean condition = ascending ? list.get(j).getRiskScore() <= pivot : list.get(j).getRiskScore() >= pivot;
            if (condition) {
                i++;
                District temp = list.get(i);
                list.set(i, list.get(j));
                list.set(j, temp);
            }
        }
        District temp = list.get(i + 1);
        list.set(i + 1, list.get(high));
        list.set(high, temp);
        return i + 1;
    }

    @Override
    public List<District> sortByName(List<District> districts, boolean ascending) {
        if (districts == null || districts.size() <= 1) return districts;
        List<District> copy = new ArrayList<>(districts);
        copy.sort((d1, d2) -> ascending ? d1.getName().compareToIgnoreCase(d2.getName()) : d2.getName().compareToIgnoreCase(d1.getName()));
        return copy;
    }

    @Override
    public List<District> searchDistricts(List<District> districts, String query) {
        if (districts == null) return new ArrayList<>();
        if (query == null || query.trim().isEmpty()) return districts;
        String q = query.trim().toLowerCase();
        return districts.stream()
                .filter(d -> d.getName().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }
}
