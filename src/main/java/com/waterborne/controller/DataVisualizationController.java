package com.waterborne.controller;

import com.waterborne.model.District;
import com.waterborne.model.RiskLevel;
import com.waterborne.model.RiskStatistics;
import com.waterborne.service.DistrictService;
import com.waterborne.util.FormatUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.Tooltip;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for DataVisualizationView.fxml.
 * Handles chart visual rendering (BarChart, PieChart, ScatterCharts) with interactive tooltips.
 */
public class DataVisualizationController implements Initializable, BaseController {

    @FXML private BarChart<String, Number> barChart;
    @FXML private PieChart pieChart;
    @FXML private ScatterChart<Number, Number> scarcityScatterChart;
    @FXML private ScatterChart<Number, Number> sanitationScatterChart;

    private DistrictService districtService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @Override
    public void initService(DistrictService service) {
        this.districtService = service;
        loadChartData();
    }

    private void loadChartData() {
        if (districtService == null) return;
        try {
            List<District> districts = districtService.getAllDistricts();
            RiskStatistics stats = districtService.getRiskStatistics();

            populateBarChart(districts);
            populatePieChart(stats);
            populateScarcityScatter(districts);
            populateSanitationScatter(districts);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void populateBarChart(List<District> districts) {
        barChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Risk Score");

        for (District d : districts) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(d.getName(), d.getRiskScore());
            series.getData().add(data);
        }

        barChart.getData().add(series);

        // Add tooltips
        for (XYChart.Data<String, Number> data : series.getData()) {
            Tooltip.install(data.getNode(), new Tooltip(data.getXValue() + "\nRisk Score: " + data.getYValue()));
        }
    }

    private void populatePieChart(RiskStatistics stats) {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        if (stats.getHighRiskCount() > 0) pieData.add(new PieChart.Data("High Risk", stats.getHighRiskCount()));
        if (stats.getModerateRiskCount() > 0) pieData.add(new PieChart.Data("Moderate Risk", stats.getModerateRiskCount()));
        if (stats.getLowRiskCount() > 0) pieData.add(new PieChart.Data("Low Risk", stats.getLowRiskCount()));

        pieChart.setData(pieData);

        for (PieChart.Data data : pieChart.getData()) {
            Tooltip.install(data.getNode(), new Tooltip(data.getName() + ": " + (int) data.getPieValue() + " districts"));
        }
    }

    private void populateScarcityScatter(List<District> districts) {
        scarcityScatterChart.getData().clear();

        XYChart.Series<Number, Number> highSeries = new XYChart.Series<>();
        highSeries.setName("High Risk");

        XYChart.Series<Number, Number> modSeries = new XYChart.Series<>();
        modSeries.setName("Moderate Risk");

        XYChart.Series<Number, Number> lowSeries = new XYChart.Series<>();
        lowSeries.setName("Low Risk");

        for (District d : districts) {
            XYChart.Data<Number, Number> data = new XYChart.Data<>(d.getWaterScarcity(), d.getRiskScore());
            if (d.getRiskLevel() == RiskLevel.HIGH) highSeries.getData().add(data);
            else if (d.getRiskLevel() == RiskLevel.MODERATE) modSeries.getData().add(data);
            else lowSeries.getData().add(data);
        }

        scarcityScatterChart.getData().addAll(highSeries, modSeries, lowSeries);
    }

    private void populateSanitationScatter(List<District> districts) {
        sanitationScatterChart.getData().clear();

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Sanitation vs Risk");

        for (District d : districts) {
            XYChart.Data<Number, Number> data = new XYChart.Data<>(d.getSanitationCoverage(), d.getRiskScore());
            series.getData().add(data);
        }

        sanitationScatterChart.getData().add(series);
    }
}
