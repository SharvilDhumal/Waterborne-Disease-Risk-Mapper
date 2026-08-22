package com.waterborne.service;

import com.waterborne.model.District;
import com.waterborne.model.RiskLevel;
import com.waterborne.model.RiskStatistics;

import java.util.List;
import java.util.Optional;

/**
 * Service Layer Interface consumed by JavaFX UI Controllers.
 * Coordinates database retrieval, risk calculations, searching, and filtering.
 */
public interface DistrictService {

    /**
     * Retrieve all districts.
     */
    List<District> getAllDistricts() throws Exception;

    /**
     * Retrieve district by name.
     */
    Optional<District> getDistrictByName(String name) throws Exception;

    /**
     * Filter districts by Risk Level and search keyword.
     */
    List<District> filterDistricts(String searchQuery, RiskLevel riskLevelFilter, String sortByOption) throws Exception;

    /**
     * Calculate dynamic statistical overview metrics.
     */
    RiskStatistics getRiskStatistics() throws Exception;

    /**
     * Retrieve districts ordered by risk score (descending).
     */
    List<District> getRiskRanking() throws Exception;
}
