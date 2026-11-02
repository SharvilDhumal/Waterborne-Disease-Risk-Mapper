package com.waterborne.controller;

import com.waterborne.model.District;
import com.waterborne.model.RiskLevel;
import com.waterborne.model.RiskStatistics;
import com.waterborne.service.DistrictService;
import com.waterborne.util.AlertUtil;
import com.waterborne.util.FormatUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for DataVisualizationView.fxml.
 * Handles high-impact public health analytics rendering:
 * - Summary KPI statistics bar
 * - Top 10 High-Risk Districts Bar Chart with risk-level coloration
 * - Nationwide Epidemiological Risk Classification Donut/Pie Chart
 * - Water Access Deficit vs Disease Risk Score Correlation Scatter
 * - Sanitation Coverage vs Disease Risk Score Correlation Scatter
 */
public class DataVisualizationController implements Initializable, BaseController {

    @FXML private Label totalDistrictsKpi;
    @FXML private Label highRiskKpi;
    @FXML private Label avgWaterDeficitKpi;
    @FXML private Label avgSanitationKpi;

    @FXML private BarChart<Number, String> barChart;
    @FXML private PieChart pieChart;
    @FXML private ScatterChart<Number, Number> scarcityScatterChart;
    @FXML private ScatterChart<Number, Number> sanitationScatterChart;

    private DistrictService districtService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Ensure analytics-chart style class is attached
        if (!barChart.getStyleClass().contains("analytics-chart")) {
            barChart.getStyleClass().add("analytics-chart");
        }
        if (!pieChart.getStyleClass().contains("analytics-chart")) {
            pieChart.getStyleClass().add("analytics-chart");
        }
        if (!scarcityScatterChart.getStyleClass().contains("analytics-chart")) {
            scarcityScatterChart.getStyleClass().add("analytics-chart");
        }
        if (!sanitationScatterChart.getStyleClass().contains("analytics-chart")) {
            sanitationScatterChart.getStyleClass().add("analytics-chart");
        }
    }

    @Override
    public void initService(DistrictService service) {
        this.districtService = service;
        loadChartData();
    }

    private void loadChartData() {
        if (districtService == null) return;
        Task<List<District>> task = new Task<>() {
            @Override
            protected List<District> call() throws Exception {
                return districtService.getAllDistricts();
            }
        };
        task.setOnSucceeded(event -> {
            List<District> districts = task.getValue();
            RiskStatistics stats = new RiskStatistics(districts);
            updateKpis(districts, stats);
            populateBarChart(districts);
            populatePieChart(stats);
            populateScarcityScatter(districts);
            populateSanitationScatter(districts);
        });
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            System.err.println("[ERROR] Failed to load analytics data: " + error.getMessage());
            AlertUtil.showError("Data Load Error", "Could Not Load Analytics",
                    "Unable to load district data: " + error.getMessage());
        });
        Thread loader = new Thread(task, "analytics-data-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private void updateKpis(List<District> districts, RiskStatistics stats) {
        if (totalDistrictsKpi != null) {
            totalDistrictsKpi.setText(String.valueOf(stats.getTotalDistricts()));
        }
        if (highRiskKpi != null) {
            highRiskKpi.setText(String.valueOf(stats.getHighRiskCount()));
        }
        if (!districts.isEmpty()) {
            double avgWaterDeficit = districts.stream()
                    .mapToDouble(District::getWaterScarcity)
                    .average()
                    .orElse(0.0);
            double avgSanitation = districts.stream()
                    .mapToDouble(District::getSanitationCoverage)
                    .average()
                    .orElse(0.0);

            if (avgWaterDeficitKpi != null) {
                avgWaterDeficitKpi.setText(String.format("%.1f%%", avgWaterDeficit));
            }
            if (avgSanitationKpi != null) {
                avgSanitationKpi.setText(String.format("%.1f%%", avgSanitation));
            }
        }
    }

    private void populateBarChart(List<District> districts) {
        barChart.getData().clear();
        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName("Composite Risk Score");

        List<District> top10 = districts.stream()
                .sorted((first, second) -> Double.compare(second.getRiskScore(), first.getRiskScore()))
                .limit(10)
                .toList();

        // In JavaFX horizontal BarChart, categories added first appear at the bottom.
        // We iterate from rank 10 down to rank 1 so that Rank #1 is at the top of the chart!
        for (int i = top10.size() - 1; i >= 0; i--) {
            District d = top10.get(i);
            int rank = i + 1;
            String category = formatDistrictRankLabel(rank, d);
            XYChart.Data<Number, String> data = new XYChart.Data<>(d.getRiskScore(), category);

            int currentRank = rank;
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    if (d.getRiskLevel() == RiskLevel.HIGH) {
                        newNode.setStyle("-fx-bar-fill: linear-gradient(to right, #ef4444, #dc2626);");
                    } else if (d.getRiskLevel() == RiskLevel.MODERATE) {
                        newNode.setStyle("-fx-bar-fill: linear-gradient(to right, #f59e0b, #d97706);");
                    } else {
                        newNode.setStyle("-fx-bar-fill: linear-gradient(to right, #10b981, #059669);");
                    }

                    String stateStr = (d.getState() != null && !d.getState().isBlank()) ? ", " + d.getState() : "";
                    String tooltipText = String.format(
                            "Rank: #%d\nDistrict: %s%s\nComposite Risk Score: %.1f / 100\nRisk Category: %s\nWater Access Deficit: %.1f%%\nSanitation Coverage: %.1f%%\nPopulation: %s",
                            currentRank, d.getName(), stateStr, d.getRiskScore(), d.getRiskLevel().getLabel(),
                            d.getWaterScarcity(), d.getSanitationCoverage(), FormatUtil.formatPopulation(d.getPopulation())
                    );
                    Tooltip tooltip = new Tooltip(tooltipText);
                    tooltip.setShowDelay(Duration.millis(80));
                    Tooltip.install(newNode, tooltip);
                }
            });

            series.getData().add(data);
        }

        barChart.getData().add(series);
    }

    private void populatePieChart(RiskStatistics stats) {
        long total = stats.getTotalDistricts();
        if (total == 0) return;

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        if (stats.getHighRiskCount() > 0) {
            double pct = (stats.getHighRiskCount() * 100.0) / total;
            pieData.add(new PieChart.Data(
                    String.format("High Risk (%d · %.1f%%)", stats.getHighRiskCount(), pct),
                    stats.getHighRiskCount()));
        }
        if (stats.getModerateRiskCount() > 0) {
            double pct = (stats.getModerateRiskCount() * 100.0) / total;
            pieData.add(new PieChart.Data(
                    String.format("Moderate Risk (%d · %.1f%%)", stats.getModerateRiskCount(), pct),
                    stats.getModerateRiskCount()));
        }
        if (stats.getLowRiskCount() > 0) {
            double pct = (stats.getLowRiskCount() * 100.0) / total;
            pieData.add(new PieChart.Data(
                    String.format("Low Risk (%d · %.1f%%)", stats.getLowRiskCount(), pct),
                    stats.getLowRiskCount()));
        }

        pieChart.setData(pieData);

        for (PieChart.Data data : pieChart.getData()) {
            String name = data.getName();
            String sliceColor;
            if (name.startsWith("High")) {
                sliceColor = "-fx-pie-color: #ef4444;";
            } else if (name.startsWith("Moderate")) {
                sliceColor = "-fx-pie-color: #f59e0b;";
            } else {
                sliceColor = "-fx-pie-color: #10b981;";
            }

            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle(sliceColor);
                    double pct = (data.getPieValue() * 100.0) / total;
                    String tooltipText = String.format(
                            "%s\nDistricts in Category: %d\nNational Share: %.1f%%",
                            name.split(" \\(")[0], (int) data.getPieValue(), pct
                    );
                    Tooltip tooltip = new Tooltip(tooltipText);
                    tooltip.setShowDelay(Duration.millis(80));
                    Tooltip.install(newNode, tooltip);
                }
            });
        }
    }

    private void populateScarcityScatter(List<District> districts) {
        scarcityScatterChart.getData().clear();

        XYChart.Series<Number, Number> highSeries = new XYChart.Series<>();
        highSeries.setName("High Risk (Score ≥ 60)");

        XYChart.Series<Number, Number> modSeries = new XYChart.Series<>();
        modSeries.setName("Moderate Risk (Score 30–59)");

        XYChart.Series<Number, Number> lowSeries = new XYChart.Series<>();
        lowSeries.setName("Low Risk (Score < 30)");

        for (District d : districts) {
            XYChart.Data<Number, Number> data = new XYChart.Data<>(d.getWaterScarcity(), d.getRiskScore());
            installScatterTooltip(data, d, "Water Deficit: " + FormatUtil.formatPercentage(d.getWaterScarcity()));

            if (d.getRiskLevel() == RiskLevel.HIGH) {
                highSeries.getData().add(data);
            } else if (d.getRiskLevel() == RiskLevel.MODERATE) {
                modSeries.getData().add(data);
            } else {
                lowSeries.getData().add(data);
            }
        }

        scarcityScatterChart.getData().addAll(highSeries, modSeries, lowSeries);
    }

    private void populateSanitationScatter(List<District> districts) {
        sanitationScatterChart.getData().clear();

        XYChart.Series<Number, Number> highSeries = new XYChart.Series<>();
        highSeries.setName("High Risk (Score ≥ 60)");

        XYChart.Series<Number, Number> modSeries = new XYChart.Series<>();
        modSeries.setName("Moderate Risk (Score 30–59)");

        XYChart.Series<Number, Number> lowSeries = new XYChart.Series<>();
        lowSeries.setName("Low Risk (Score < 30)");

        for (District d : districts) {
            XYChart.Data<Number, Number> data = new XYChart.Data<>(d.getSanitationCoverage(), d.getRiskScore());
            installScatterTooltip(data, d, "Sanitation Coverage: " + FormatUtil.formatPercentage(d.getSanitationCoverage()));

            if (d.getRiskLevel() == RiskLevel.HIGH) {
                highSeries.getData().add(data);
            } else if (d.getRiskLevel() == RiskLevel.MODERATE) {
                modSeries.getData().add(data);
            } else {
                lowSeries.getData().add(data);
            }
        }

        sanitationScatterChart.getData().addAll(highSeries, modSeries, lowSeries);
    }

    private void installScatterTooltip(XYChart.Data<Number, Number> data, District d, String extraMetric) {
        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                String stateStr = (d.getState() != null && !d.getState().isBlank()) ? ", " + d.getState() : "";
                String tooltipText = String.format(
                        "District: %s%s\nRisk Category: %s\nComposite Score: %.1f / 100\n%s\nSanitation: %.1f%%\nPopulation: %s",
                        d.getName(), stateStr, d.getRiskLevel().getLabel(), d.getRiskScore(), extraMetric,
                        d.getSanitationCoverage(), FormatUtil.formatPopulation(d.getPopulation())
                );
                Tooltip tooltip = new Tooltip(tooltipText);
                tooltip.setShowDelay(Duration.millis(80));
                Tooltip.install(newNode, tooltip);
            }
        });
    }

    private String formatDistrictRankLabel(int rank, District d) {
        String name = d.getName();
        if (name == null) return "#" + rank;
        name = name.replace("Twenty Four", "24")
                   .replace("AND", "&")
                   .replace("Sri Potti Sriramulu ", "SPS ")
                   .replace("Leh(Ladakh)", "Leh (Ladakh)");

        String state = d.getState();
        if (state != null && !state.isBlank()) {
            state = toTitleCase(state.trim())
                    .replace("Jammu And Kashmir", "J&K")
                    .replace("Dadra And Nagar Haveli", "DNH")
                    .replace("Daman And Diu", "Daman & Diu");
            return String.format("#%d  %s (%s)", rank, name, state);
        }
        return String.format("#%d  %s", rank, name);
    }

    private static String toTitleCase(String text) {
        if (text == null || text.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (!word.isBlank()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
