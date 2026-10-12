package com.waterborne.service;

import com.waterborne.algorithm.RiskCalculatorService;
import com.waterborne.model.District;
import com.waterborne.model.RiskLevel;
import com.waterborne.model.RiskStatistics;
import com.waterborne.repository.DistrictRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Production implementation of DistrictService.
 * Integrates a district repository and the risk ranking/search algorithms.
 */
public class DistrictServiceImpl implements DistrictService {

    private final DistrictRepository repository;
    private final RiskCalculatorService riskCalculator;

    public DistrictServiceImpl(DistrictRepository repository, RiskCalculatorService riskCalculator) {
        this.repository = repository;
        this.riskCalculator = riskCalculator;
    }

    @Override
    public List<District> getAllDistricts() throws Exception {
        List<District> districts = repository.findAll();
        return districts;
    }

    @Override
    public Optional<District> getDistrictByName(String name) throws Exception {
        List<District> all = getAllDistricts();
        return all.stream()
                .filter(d -> d.getName().equalsIgnoreCase(name != null ? name.trim() : ""))
                .findFirst();
    }

    @Override
    public List<District> filterDistricts(String searchQuery, RiskLevel riskLevelFilter, String sortByOption) throws Exception {
        List<District> list = getAllDistricts();
        if (riskCalculator != null) {
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                list = riskCalculator.searchDistricts(list, searchQuery.trim());
            }
        } else {
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                String q = searchQuery.trim().toLowerCase();
                list = list.stream().filter(d -> d.getName().toLowerCase().contains(q)).collect(Collectors.toList());
            }
        }

        if (riskLevelFilter != null) {
            list = list.stream().filter(d -> d.getRiskLevel() == riskLevelFilter).collect(Collectors.toList());
        }

        if (riskCalculator != null && sortByOption != null) {
            if (sortByOption.contains("High to Low")) {
                list = riskCalculator.sortByRiskScore(list, false);
            } else if (sortByOption.contains("Low to High")) {
                list = riskCalculator.sortByRiskScore(list, true);
            } else if (sortByOption.contains("A-Z")) {
                list = riskCalculator.sortByName(list, true);
            }
        }
        return list;
    }

    @Override
    public RiskStatistics getRiskStatistics() throws Exception {
        return new RiskStatistics(getAllDistricts());
    }

    @Override
    public List<District> getRiskRanking() throws Exception {
        List<District> districts = getAllDistricts();
        if (riskCalculator != null) {
            return riskCalculator.sortByRiskScore(districts, false);
        }
        return districts;
    }
}
