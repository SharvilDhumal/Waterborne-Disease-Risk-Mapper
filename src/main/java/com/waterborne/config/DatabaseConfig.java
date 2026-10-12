package com.waterborne.config;

import com.waterborne.algorithm.RiskCalculatorServiceImpl;
import com.waterborne.repository.CensusAndNfhs5DistrictRepository;
import com.waterborne.service.DistrictService;
import com.waterborne.service.DistrictServiceImpl;
import com.waterborne.service.MockDistrictDataService;
import com.waterborne.repository.DistrictRepositoryImpl;

/**
 * Creates the configured district data service without storing credentials in source.
 */
public final class DatabaseConfig {

    public enum DataSource {
        CENSUS_AND_NFHS5,
        MYSQL,
        MOCK
    }

    private DatabaseConfig() {
    }

    public static DataSource getDataSource() {
        String configuredSource = System.getenv().getOrDefault("WATERBORNE_DATA_SOURCE", "census").trim();
        return switch (configuredSource.toLowerCase()) {
            case "census", "csv", "nfhs", "census+nfhs" -> DataSource.CENSUS_AND_NFHS5;
            case "mysql" -> DataSource.MYSQL;
            case "mock" -> DataSource.MOCK;
            default -> throw new IllegalStateException(
                    "WATERBORNE_DATA_SOURCE must be 'census', 'mysql', or 'mock'.");
        };
    }

    public static DistrictService createDistrictService(DataSource dataSource) {
        if (dataSource == DataSource.MOCK) {
            return new MockDistrictDataService();
        }
        if (dataSource == DataSource.CENSUS_AND_NFHS5) {
            return new DistrictServiceImpl(
                    new CensusAndNfhs5DistrictRepository(),
                    new RiskCalculatorServiceImpl());
        }

        return new DistrictServiceImpl(
                new DistrictRepositoryImpl(),
                new RiskCalculatorServiceImpl());
    }
}
