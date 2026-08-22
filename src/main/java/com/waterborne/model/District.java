package com.waterborne.model;

import java.util.Objects;

/**
 * Domain Model for District Environmental & Public Health Data.
 * Contains indicators like water scarcity, sanitation, population, and calculated risk metrics.
 */
public class District {
    private int id;
    private String name;
    private double waterScarcity; // Percentage (0 - 100%)
    private double sanitationCoverage; // Percentage (0 - 100%)
    private long population;
    private double populationDensity; // People per sq km
    private double riskScore; // 0 - 100
    private RiskLevel riskLevel;

    public District() {
    }

    public District(int id, String name, double waterScarcity, double sanitationCoverage, long population, double populationDensity) {
        this.id = id;
        this.name = name;
        this.waterScarcity = waterScarcity;
        this.sanitationCoverage = sanitationCoverage;
        this.population = population;
        this.populationDensity = populationDensity;
        recalculateRisk();
    }

    public District(int id, String name, double waterScarcity, double sanitationCoverage, long population, double populationDensity, double riskScore, RiskLevel riskLevel) {
        this.id = id;
        this.name = name;
        this.waterScarcity = waterScarcity;
        this.sanitationCoverage = sanitationCoverage;
        this.population = population;
        this.populationDensity = populationDensity;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
    }

    public void recalculateRisk() {
        // Fallback formula if risk score is calculated directly on model
        // Risk Score = 0.45 * Water Scarcity + 0.35 * (100 - Sanitation) + 0.20 * Density Index
        double densityIndex = Math.min(100.0, (this.populationDensity / 1500.0) * 100.0);
        this.riskScore = Math.round((0.45 * this.waterScarcity + 0.35 * (100.0 - this.sanitationCoverage) + 0.20 * densityIndex) * 10.0) / 10.0;
        this.riskLevel = RiskLevel.fromScore(this.riskScore);
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getWaterScarcity() {
        return waterScarcity;
    }

    public void setWaterScarcity(double waterScarcity) {
        this.waterScarcity = waterScarcity;
    }

    public double getSanitationCoverage() {
        return sanitationCoverage;
    }

    public void setSanitationCoverage(double sanitationCoverage) {
        this.sanitationCoverage = sanitationCoverage;
    }

    public long getPopulation() {
        return population;
    }

    public void setPopulation(long population) {
        this.population = population;
    }

    public double getPopulationDensity() {
        return populationDensity;
    }

    public void setPopulationDensity(double populationDensity) {
        this.populationDensity = populationDensity;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(double riskScore) {
        this.riskScore = riskScore;
        this.riskLevel = RiskLevel.fromScore(riskScore);
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        District district = (District) o;
        return id == district.id || Objects.equals(name, district.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return String.format("%s [Score: %.1f, Level: %s]", name, riskScore, riskLevel);
    }
}
