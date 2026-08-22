package com.waterborne.algorithm;

import com.waterborne.model.District;
import com.waterborne.model.RiskLevel;

import java.util.List;

/**
 * Interface for Data Processing & Algorithm logic.
 * Implemented by Student 1 (Data & Algorithm Engineer).
 * Exposes methods for sorting, searching, risk scoring, and statistical logic.
 */
public interface RiskCalculatorService {

    /**
     * Compute risk score (0-100) based on environmental indicators.
     */
    double calculateRiskScore(double waterScarcity, double sanitationCoverage, long population, double populationDensity);

    /**
     * Classify risk level based on numerical score.
     */
    RiskLevel classifyRisk(double riskScore);

    /**
     * Sort districts by risk score using custom sorting algorithms (QuickSort / MergeSort).
     */
    List<District> sortByRiskScore(List<District> districts, boolean ascending);

    /**
     * Sort districts by name alphabetically using custom sorting algorithms.
     */
    List<District> sortByName(List<District> districts, boolean ascending);

    /**
     * Perform case-insensitive search matching using searching algorithms.
     */
    List<District> searchDistricts(List<District> districts, String query);
}
