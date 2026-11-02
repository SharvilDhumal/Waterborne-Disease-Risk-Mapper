package com.waterborne.model;

public record WaterQualityRecord(
        String stationCode,
        String location,
        String waterBodyType,
        String state,
        Double temperatureMin,
        Double temperatureMax,
        Double dissolvedOxygenMin,
        Double dissolvedOxygenMax,
        Double phMin,
        Double phMax,
        Double conductivityMin,
        Double conductivityMax,
        Double bodMin,
        Double bodMax,
        Double nitrateMin,
        Double nitrateMax,
        Double fecalColiformMin,
        Double fecalColiformMax,
        Double totalColiformMin,
        Double totalColiformMax) {

    public Double averageTemperature() {
        return average(temperatureMin, temperatureMax);
    }

    public Double averageDissolvedOxygen() {
        return average(dissolvedOxygenMin, dissolvedOxygenMax);
    }

    public Double averagePh() {
        return average(phMin, phMax);
    }

    public Double averageConductivity() {
        return average(conductivityMin, conductivityMax);
    }

    public Double averageBod() {
        return average(bodMin, bodMax);
    }

    public Double averageNitrate() {
        return average(nitrateMin, nitrateMax);
    }

    public Double averageFecalColiform() {
        return average(fecalColiformMin, fecalColiformMax);
    }

    public Double averageTotalColiform() {
        return average(totalColiformMin, totalColiformMax);
    }

    private static Double average(Double minimum, Double maximum) {
        if (minimum == null) {
            return maximum;
        }
        if (maximum == null) {
            return minimum;
        }
        return (minimum + maximum) / 2.0;
    }
}
