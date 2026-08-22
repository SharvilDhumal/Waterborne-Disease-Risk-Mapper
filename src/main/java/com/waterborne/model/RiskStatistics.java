package com.waterborne.model;

import java.util.List;

/**
 * Encapsulates aggregated statistical metadata computed across all districts.
 * Used for dynamic UI dashboard KPI updates.
 */
public class RiskStatistics {
    private final int totalDistricts;
    private final long highRiskCount;
    private final long moderateRiskCount;
    private final long lowRiskCount;
    private final double averageRiskScore;
    private final double maxRiskScore;
    private final double minRiskScore;
    private final District highestRiskDistrict;
    private final District lowestRiskDistrict;

    public RiskStatistics(List<District> districts) {
        if (districts == null || districts.isEmpty()) {
            this.totalDistricts = 0;
            this.highRiskCount = 0;
            this.moderateRiskCount = 0;
            this.lowRiskCount = 0;
            this.averageRiskScore = 0.0;
            this.maxRiskScore = 0.0;
            this.minRiskScore = 0.0;
            this.highestRiskDistrict = null;
            this.lowestRiskDistrict = null;
            return;
        }

        this.totalDistricts = districts.size();
        this.highRiskCount = districts.stream().filter(d -> d.getRiskLevel() == RiskLevel.HIGH).count();
        this.moderateRiskCount = districts.stream().filter(d -> d.getRiskLevel() == RiskLevel.MODERATE).count();
        this.lowRiskCount = districts.stream().filter(d -> d.getRiskLevel() == RiskLevel.LOW).count();

        double sumScore = 0.0;
        double maxScore = Double.NEGATIVE_INFINITY;
        double minScore = Double.POSITIVE_INFINITY;
        District highest = null;
        District lowest = null;

        for (District d : districts) {
            double score = d.getRiskScore();
            sumScore += score;

            if (score > maxScore) {
                maxScore = score;
                highest = d;
            }
            if (score < minScore) {
                minScore = score;
                lowest = d;
            }
        }

        this.averageRiskScore = Math.round((sumScore / districts.size()) * 10.0) / 10.0;
        this.maxRiskScore = maxScore;
        this.minRiskScore = minScore;
        this.highestRiskDistrict = highest;
        this.lowestRiskDistrict = lowest;
    }

    public int getTotalDistricts() {
        return totalDistricts;
    }

    public long getHighRiskCount() {
        return highRiskCount;
    }

    public long getModerateRiskCount() {
        return moderateRiskCount;
    }

    public long getLowRiskCount() {
        return lowRiskCount;
    }

    public double getAverageRiskScore() {
        return averageRiskScore;
    }

    public double getMaxRiskScore() {
        return maxRiskScore;
    }

    public double getMinRiskScore() {
        return minRiskScore;
    }

    public District getHighestRiskDistrict() {
        return highestRiskDistrict;
    }

    public District getLowestRiskDistrict() {
        return lowestRiskDistrict;
    }
}
