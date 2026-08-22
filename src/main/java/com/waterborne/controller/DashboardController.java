package com.waterborne.controller;

import com.waterborne.model.District;
import com.waterborne.model.RiskLevel;
import com.waterborne.model.RiskStatistics;
import com.waterborne.service.DistrictService;
import com.waterborne.util.FormatUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.List;
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
                stats = districtService.getRiskStatistics();
                districts = districtService.getAllDistricts();
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> updateUI(stats, districts));
            }

            @Override
            protected void failed() {
                System.err.println("[ERROR] Failed to load dashboard stats: " + getException().getMessage());
            }
        };

        new Thread(task).start();
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

        // Bar Chart (District Risk Comparison)
        riskBarChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Risk Score");

        // Display top 8 districts in bar chart
        districts.stream()
                .sorted((d1, d2) -> Double.compare(d2.getRiskScore(), d1.getRiskScore()))
                .limit(8)
                .forEach(d -> series.getData().add(new XYChart.Data<>(d.getName(), d.getRiskScore())));

        riskBarChart.getData().add(series);

        // Pie Chart (Distribution)
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
}
