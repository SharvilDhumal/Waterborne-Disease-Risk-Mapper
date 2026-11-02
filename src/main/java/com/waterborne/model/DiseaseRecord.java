package com.waterborne.model;

public record DiseaseRecord(
        String country,
        String region,
        Integer year,
        String waterSourceType,
        Double contaminantLevel,
        Double cleanWaterAccess,
        Double sanitationCoverage,
        Double diarrhealCases,
        Double choleraCases,
        Double typhoidCases) {
}
