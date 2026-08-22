package com.waterborne.model;

/**
 * Represents the risk level classification of a district.
 * Used consistently across Dashboard, Table, Details, and Charts.
 */
public enum RiskLevel {
    LOW("LOW RISK", "#10B981", "#D1FAE5"),
    MODERATE("MODERATE RISK", "#F59E0B", "#FEF3C7"),
    HIGH("HIGH RISK", "#EF4444", "#FEE2E2");

    private final String label;
    private final String primaryColor;
    private final String backgroundColor;

    RiskLevel(String label, String primaryColor, String backgroundColor) {
        this.label = label;
        this.primaryColor = primaryColor;
        this.backgroundColor = backgroundColor;
    }

    public String getLabel() {
        return label;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public static RiskLevel fromScore(double score) {
        if (score >= 70.0) {
            return HIGH;
        } else if (score >= 40.0) {
            return MODERATE;
        } else {
            return LOW;
        }
    }

    @Override
    public String toString() {
        return label;
    }
}
