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
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;

import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller for DashboardView.fxml.
 * Handles dynamic KPI updates, spotlight cards, and chart visual data binding.
 */
public class DashboardController implements Initializable, BaseController {

    @FXML private Label totalDistrictsLabel;
    @FXML private Label highRiskLabel;
    @FXML private Label moderateRiskLabel;
    @FXML private Label lowRiskLabel;

    @FXML private Label spotlightNameLabel;
    @FXML private Label spotlightStateLabel;
    @FXML private Label spotlightScoreLabel;
    @FXML private Label spotlightBadgeLabel;
    @FXML private Label spotlightScarcityLabel;
    @FXML private Label spotlightSanitationDeficitLabel;
    @FXML private Label spotlightPopulationLabel;
    @FXML private ProgressBar spotlightScarcityBar;
    @FXML private ProgressBar spotlightSanitationBar;

    @FXML private Label avgScoreLabel;

    @FXML private BarChart<Number, String> riskBarChart;
    @FXML private BarChart<Number, String> deficitBarChart;
    @FXML private PieChart riskPieChart;

    private DistrictService districtService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Chart styling hooks
        riskBarChart.getStyleClass().add("analytics-chart");
        deficitBarChart.getStyleClass().add("analytics-chart");
        riskPieChart.getStyleClass().add("analytics-chart");
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

        // --- KPI counters ---
        totalDistrictsLabel.setText(String.valueOf(stats.getTotalDistricts()));
        highRiskLabel.setText(String.valueOf(stats.getHighRiskCount()));
        moderateRiskLabel.setText(String.valueOf(stats.getModerateRiskCount()));
        lowRiskLabel.setText(String.valueOf(stats.getLowRiskCount()));
        avgScoreLabel.setText(FormatUtil.formatScore(stats.getAverageRiskScore()));

        // --- Spotlight ---
        District highest = stats.getHighestRiskDistrict();
        if (highest != null) {
            spotlightNameLabel.setText(highest.getName());
            spotlightStateLabel.setText(highest.getState() == null || highest.getState().isBlank()
                    ? "State / UT not specified"
                    : highest.getState());
            spotlightScoreLabel.setText("Risk score: "
                    + FormatUtil.formatScore(highest.getRiskScore()) + " / 100");

            double waterDeficit = highest.getWaterScarcity();
            double sanitationDeficit = 100.0 - highest.getSanitationCoverage();

            spotlightScarcityLabel.setText(FormatUtil.formatPercentage(waterDeficit));
            spotlightSanitationDeficitLabel.setText(FormatUtil.formatPercentage(sanitationDeficit));
            spotlightPopulationLabel.setText(FormatUtil.formatPopulation(highest.getPopulation()));

            // Mini progress bars: use maxWidth to encode proportion
            applyProgress(spotlightScarcityBar, waterDeficit / 100.0, "kpi-bar-high");
            applyProgress(spotlightSanitationBar, sanitationDeficit / 100.0, "kpi-bar-moderate");

            RiskLevel level = highest.getRiskLevel();
            spotlightBadgeLabel.setText(level.getLabel());
            spotlightBadgeLabel.getStyleClass().removeAll("badge-low", "badge-moderate", "badge-high");
            if (level == RiskLevel.HIGH) spotlightBadgeLabel.getStyleClass().add("badge-high");
            else if (level == RiskLevel.MODERATE) spotlightBadgeLabel.getStyleClass().add("badge-moderate");
            else spotlightBadgeLabel.getStyleClass().add("badge-low");
        }

        // --- Top 10 risk horizontal bar chart (composite score) ---
        riskBarChart.getData().clear();
        XYChart.Series<Number, String> scoreSeries = new XYChart.Series<>();
        scoreSeries.setName("Composite risk score");

        // --- Top 10 deficit horizontal bar chart (water vs sanitation) ---
        deficitBarChart.getData().clear();
        XYChart.Series<Number, String> waterSeries = new XYChart.Series<>();
        waterSeries.setName("Water deficit");
        XYChart.Series<Number, String> sanitationSeries = new XYChart.Series<>();
        sanitationSeries.setName("Sanitation deficit");

        List<District> top10 = districts.stream()
                .sorted((d1, d2) -> Double.compare(d2.getRiskScore(), d1.getRiskScore()))
                .limit(10)
                .toList();

        // In JavaFX horizontal BarChart, categories added first appear at the bottom.
        // We iterate from rank 10 down to rank 1 so that Rank #1 is at the top!
        for (int i = top10.size() - 1; i >= 0; i--) {
            District district = top10.get(i);
            int rank = i + 1;
            String category = formatDistrictRankLabel(rank, district);

            // Score bar (X=score, Y=category)
            XYChart.Data<Number, String> scoreData =
                    new XYChart.Data<>(district.getRiskScore(), category);
            installTooltip(scoreData,
                    district.getName() + (district.getState() == null ? "" : ", " + district.getState())
                            + "\nRank: #" + rank
                            + "\nComposite risk score: " + FormatUtil.formatScore(district.getRiskScore())
                            + " / 100"
                            + "\nCategory: " + district.getRiskLevel().getLabel());
            scoreSeries.getData().add(scoreData);

            // Deficit bars (X=deficit, Y=category)
            double waterDeficit = district.getWaterScarcity();
            double sanitationDeficit = 100.0 - district.getSanitationCoverage();

            XYChart.Data<Number, String> waterData =
                    new XYChart.Data<>(waterDeficit, category);
            installTooltip(waterData,
                    district.getName() + " — Water deficit\n"
                            + FormatUtil.formatPercentage(waterDeficit));
            waterSeries.getData().add(waterData);

            XYChart.Data<Number, String> sanitationData =
                    new XYChart.Data<>(sanitationDeficit, category);
            installTooltip(sanitationData,
                    district.getName() + " — Sanitation deficit\n"
                            + FormatUtil.formatPercentage(sanitationDeficit));
            sanitationSeries.getData().add(sanitationData);
        }

        // Highlight the highest-risk bar (Rank #1 is the last item in series data, at the top)
        if (!scoreSeries.getData().isEmpty()) {
            XYChart.Data<Number, String> topBar = scoreSeries.getData().get(scoreSeries.getData().size() - 1);
            topBar.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) newNode.getStyleClass().add("bar-highlight");
            });
        }

        riskBarChart.getData().add(scoreSeries);
        deficitBarChart.getData().addAll(waterSeries, sanitationSeries);

        // --- Donut chart ---
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        if (stats.getHighRiskCount() > 0) {
            PieChart.Data d = new PieChart.Data(
                    "High (" + stats.getHighRiskCount() + ")", stats.getHighRiskCount());
            pieData.add(d);
        }
        if (stats.getModerateRiskCount() > 0) {
            PieChart.Data d = new PieChart.Data(
                    "Moderate (" + stats.getModerateRiskCount() + ")", stats.getModerateRiskCount());
            pieData.add(d);
        }
        if (stats.getLowRiskCount() > 0) {
            PieChart.Data d = new PieChart.Data(
                    "Low (" + stats.getLowRiskCount() + ")", stats.getLowRiskCount());
            pieData.add(d);
        }
        riskPieChart.setData(pieData);

        // Color slices after nodes exist
        for (PieChart.Data d : pieData) {
            String name = d.getName();
            String color;
            if (name.startsWith("High")) color = "-fx-pie-color: #DC4C4C;";
            else if (name.startsWith("Moderate")) color = "-fx-pie-color: #E6A23C;";
            else color = "-fx-pie-color: #36A675;";
            d.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) newNode.setStyle(color);
            });
        }
    }

    private void applyProgress(ProgressBar bar, double fraction, String colorClass) {
        if (bar == null) return;
        double clamped = Math.max(0.0, Math.min(1.0, fraction));
        bar.setProgress(clamped);
        bar.getStyleClass().removeAll("kpi-bar-high", "kpi-bar-moderate", "kpi-bar-low");
        bar.getStyleClass().add(colorClass);
    }

    private void installTooltip(XYChart.Data<Number, String> data, String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setShowDelay(javafx.util.Duration.millis(80));
        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) Tooltip.install(newNode, tooltip);
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