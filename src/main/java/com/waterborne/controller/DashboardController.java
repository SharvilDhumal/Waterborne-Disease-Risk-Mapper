package com.waterborne.controller;

import com.waterborne.model.District;
import com.waterborne.model.RiskLevel;
import com.waterborne.model.RiskStatistics;
import com.waterborne.service.DistrictService;
import com.waterborne.util.AlertUtil;
import com.waterborne.util.FormatUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller for DashboardView.fxml.
 * Handles dynamic KPI updates, spotlight cards, and summary chart visual data binding.
 */
public class DashboardController implements Initializable, BaseController {

    @FXML private Label totalDistrictsLabel;
    @FXML private Label highRiskLabel;
    @FXML private Label moderateRiskLabel;
    @FXML private Label lowRiskLabel;

    @FXML private Label spotlightNameLabel;
    @FXML private Label spotlightScoreLabel;
    @FXML private Label spotlightBadgeLabel;
    @FXML private Label spotlightScarcityLabel;
    @FXML private Label spotlightSanitationLabel;
    @FXML private Label spotlightPopulationLabel;

    @FXML private Label avgScoreLabel;

    @FXML private BarChart<String, Number> riskBarChart;
    @FXML private PieChart riskPieChart;

    private DistrictService districtService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @Override
    public void initService(DistrictService service) {
        this.districtService = service;
        loadDashboardDataAsync();
    }

    private void loadDashboardDataAsync() {
        if (districtService == null) return;

        Task<Void> task = new Task<>() {
            private RiskStatistics stats;
            private List<District> districts;

            @Override
            protected Void call() throws Exception {
                districts = districtService.getAllDistricts();
                stats = new RiskStatistics(districts);
                return null;
            }

            @Override
            protected void succeeded() {
                updateUI(stats, districts);
            }

            @Override
            protected void failed() {
                Throwable error = getException();
                System.err.println("[ERROR] Failed to load dashboard stats: " + error.getMessage());
                AlertUtil.showError("Data Load Error", "Could Not Load Dashboard",
                        "Unable to load district data: " + error.getMessage());
            }
        };

        Thread loader = new Thread(task, "dashboard-data-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private void updateUI(RiskStatistics stats, List<District> districts) {
        if (stats == null || districts == null) return;

        // KPI Counters
        totalDistrictsLabel.setText(String.valueOf(stats.getTotalDistricts()));
        highRiskLabel.setText(String.valueOf(stats.getHighRiskCount()));
        moderateRiskLabel.setText(String.valueOf(stats.getModerateRiskCount()));
        lowRiskLabel.setText(String.valueOf(stats.getLowRiskCount()));
        avgScoreLabel.setText(FormatUtil.formatScore(stats.getAverageRiskScore()));

        // Spotlight Card
        District highest = stats.getHighestRiskDistrict();
        if (highest != null) {
            spotlightNameLabel.setText(highest.getName());
            spotlightScoreLabel.setText("Score: " + FormatUtil.formatScore(highest.getRiskScore()) + " / 100");
            spotlightScarcityLabel.setText(FormatUtil.formatPercentage(highest.getWaterScarcity()));
            spotlightSanitationLabel.setText(FormatUtil.formatPercentage(highest.getSanitationCoverage()));
            spotlightPopulationLabel.setText(FormatUtil.formatPopulation(highest.getPopulation()));

            RiskLevel level = highest.getRiskLevel();
            spotlightBadgeLabel.setText(level.getLabel());
            spotlightBadgeLabel.getStyleClass().removeAll("badge-low", "badge-moderate", "badge-high");
            if (level == RiskLevel.HIGH) spotlightBadgeLabel.getStyleClass().add("badge-high");
            else if (level == RiskLevel.MODERATE) spotlightBadgeLabel.getStyleClass().add("badge-moderate");
            else spotlightBadgeLabel.getStyleClass().add("badge-low");
        }

        // Compare the source-based risk inputs for the districts with the highest composite scores.
        riskBarChart.getData().clear();
        XYChart.Series<String, Number> waterDeficitSeries = new XYChart.Series<>();
        waterDeficitSeries.setName("Water-risk deficit");
        XYChart.Series<String, Number> sanitationDeficitSeries = new XYChart.Series<>();
        sanitationDeficitSeries.setName("Sanitation deficit");

        districts.stream()
                .sorted((d1, d2) -> Double.compare(d2.getRiskScore(), d1.getRiskScore()))
                .limit(10)
                .forEach(district -> {
                    String category = district.getState() == null || district.getState().isBlank()
                            ? district.getName()
                            : district.getName() + " (" + district.getState() + ")";
                    String source = district.hasNfhs5Metrics()
                            ? "NFHS-5"
                            : Double.isFinite(district.getCensusWaterScarcity())
                                    ? "Census 2011"
                                    : "Configured data source";
                    double waterDeficit = district.getWaterScarcity();
                    double sanitationDeficit = 100.0 - district.getSanitationCoverage();
                    XYChart.Data<String, Number> waterData =
                            new XYChart.Data<>(category, waterDeficit);
                    XYChart.Data<String, Number> sanitationData =
                            new XYChart.Data<>(category, sanitationDeficit);
                    installMetricTooltip(waterData, district, "Water-risk deficit", waterDeficit, source);
                    installMetricTooltip(sanitationData, district, "Sanitation deficit", sanitationDeficit, source);
                    waterDeficitSeries.getData().add(waterData);
                    sanitationDeficitSeries.getData().add(sanitationData);
                });

        riskBarChart.getData().addAll(waterDeficitSeries, sanitationDeficitSeries);

        // Pie Chart (district counts by composite risk-score category)
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        if (stats.getHighRiskCount() > 0) {
            pieData.add(new PieChart.Data("High Risk (" + stats.getHighRiskCount() + ")", stats.getHighRiskCount()));
        }
        if (stats.getModerateRiskCount() > 0) {
            pieData.add(new PieChart.Data("Moderate Risk (" + stats.getModerateRiskCount() + ")", stats.getModerateRiskCount()));
        }
        if (stats.getLowRiskCount() > 0) {
            pieData.add(new PieChart.Data("Low Risk (" + stats.getLowRiskCount() + ")", stats.getLowRiskCount()));
        }
        riskPieChart.setData(pieData);
    }

    private void installMetricTooltip(XYChart.Data<String, Number> data, District district,
                                      String metric, double value, String source) {
        String location = district.getState() == null || district.getState().isBlank()
                ? district.getName()
                : district.getName() + ", " + district.getState();
        Tooltip tooltip = new Tooltip(String.format(
                Locale.ROOT, "%s\n%s: %.2f%%\nSource: %s", location, metric, value, source));
        data.nodeProperty().addListener((observable, oldNode, newNode) -> {
            if (newNode != null) {
                Tooltip.install(newNode, tooltip);
            }
        });
    }
}
