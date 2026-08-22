package com.waterborne.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility functions for consistent number, percentage, and text formatting.
 */
public class FormatUtil {

    private static final NumberFormat POPULATION_FORMATter = NumberFormat.getInstance(Locale.US);

    public static String formatPopulation(long population) {
        if (population >= 1_000_000) {
            return String.format(Locale.US, "%.1fM", population / 1_000_000.0);
        } else if (population >= 1_000) {
            return String.format(Locale.US, "%.1fK", population / 1_000.0);
        }
        return POPULATION_FORMATter.format(population);
    }

    public static String formatPercentage(double value) {
        return String.format(Locale.US, "%.1f%%", value);
    }

    public static String formatScore(double score) {
        return String.format(Locale.US, "%.1f", score);
    }

    public static String formatDensity(double density) {
        return String.format(Locale.US, "%.0f / km²", density);
    }
}
