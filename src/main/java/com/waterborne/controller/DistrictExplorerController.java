package com.waterborne.controller;

import com.waterborne.model.District;
import com.waterborne.model.RiskLevel;
import com.waterborne.service.DistrictService;
import com.waterborne.util.AlertUtil;
import com.waterborne.util.FormatUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller for DistrictExplorerView.fxml.
 * Handles district search, list rendering, profile inspection,
 * contribution chart, source comparison and automated interpretation.
 */
public class DistrictExplorerController implements Initializable, BaseController {

    @FXML private TextField searchField;
    @FXML private ListView<District> districtListView;
    @FXML private Label districtCountLabel;

    @FXML private Label districtNameLabel;
    @FXML private Label districtStateLabel;
    @FXML private Label riskScoreLabel;
    @FXML private Label riskBadgeLabel;

    @FXML private Label scarcityValueLabel;
    @FXML private Label sanitationValueLabel;
    @FXML private Label densityValueLabel;
    @FXML private ProgressBar waterProgressBar;
    @FXML private ProgressBar sanitationProgressBar;
    @FXML private ProgressBar densityProgressBar;


    @FXML private BarChart<Number, String> contributionChart;
    @FXML private Label interpretationLabel;

    private DistrictService districtService;
    private final ObservableList<District> masterDistricts = FXCollections.observableArrayList();
    private FilteredList<District> filteredDistricts;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupListView();
        setupSearchListener();
        contributionChart.getStyleClass().add("analytics-chart");
    }

    @Override
    public void initService(DistrictService service) {
        this.districtService = service;
        loadDistricts();
    }

    private void setupListView() {
        filteredDistricts = new FilteredList<>(masterDistricts, d -> true);
        districtListView.setItems(filteredDistricts);
        districtListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(District item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label name = new Label(item.getName());
                    name.getStyleClass().add("district-cell-name");
                    name.setWrapText(true);
                    name.setMaxWidth(170.0);

                    Label state = new Label(item.getState() == null ? "—" : item.getState());
                    state.getStyleClass().add("district-cell-state");
                    state.setWrapText(true);
                    state.setMaxWidth(170.0);

                    VBox text = new VBox(2.0, name, state);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label badge = new Label(item.getRiskLevel().getLabel());
                    badge.getStyleClass().add("badge");
                    badge.setMinWidth(Region.USE_PREF_SIZE);
                    if (item.getRiskLevel() == RiskLevel.HIGH) badge.getStyleClass().add("badge-high");
                    else if (item.getRiskLevel() == RiskLevel.MODERATE) badge.getStyleClass().add("badge-moderate");
                    else badge.getStyleClass().add("badge-low");

                    HBox container = new HBox(8.0, text, spacer, badge);
                    container.setAlignment(Pos.CENTER_LEFT);
                    container.setMaxWidth(Double.MAX_VALUE);
                    setGraphic(container);
                }
            }
        });

        districtListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> {
                    if (newSel != null) displayDistrictDetails(newSel);
                });
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, oldV, newV) -> applySearch(newV));
    }

    private void loadDistricts() {
        if (districtService == null) return;
        Task<List<District>> task = new Task<>() {
            @Override
            protected List<District> call() throws Exception {
                return districtService.getAllDistricts();
            }
        };
        task.setOnSucceeded(e -> {
            masterDistricts.setAll(task.getValue());
            applySearch(searchField.getText());
            if (!masterDistricts.isEmpty()) {
                districtListView.getSelectionModel().selectFirst();
            }
        });
        task.setOnFailed(e -> {
            Throwable err = task.getException();
            System.err.println("[ERROR] Failed to load districts: " + err.getMessage());
            AlertUtil.showError("Data Load Error", "Could Not Load Districts",
                    "Unable to load district data: " + err.getMessage());
        });

        Thread t = new Thread(task, "district-explorer-data-loader");
        t.setDaemon(true);
        t.start();
    }

    private void applySearch(String text) {
        String query = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        filteredDistricts.setPredicate(d ->
                query.isEmpty()
                        || d.getName().toLowerCase(Locale.ROOT).contains(query)
                        || (d.getState() != null
                            && d.getState().toLowerCase(Locale.ROOT).contains(query)));

        districtCountLabel.setText(filteredDistricts.size() + " of "
                + masterDistricts.size() + " districts");

        if (!filteredDistricts.isEmpty()) {
            districtListView.getSelectionModel().selectFirst();
        } else {
            districtListView.getSelectionModel().clearSelection();
        }
    }

    private void displayDistrictDetails(District district) {
        if (district == null) return;

        districtNameLabel.setText(district.getName());
        districtStateLabel.setText(district.getState() == null || district.getState().isBlank()
                ? "State / UT not specified"
                : district.getState());
        riskScoreLabel.setText(FormatUtil.formatScore(district.getRiskScore()) + " / 100");

        RiskLevel level = district.getRiskLevel();
        riskBadgeLabel.setText(level.getLabel());
        riskBadgeLabel.getStyleClass().removeAll("badge-low", "badge-moderate", "badge-high");
        if (level == RiskLevel.HIGH) riskBadgeLabel.getStyleClass().add("badge-high");
        else if (level == RiskLevel.MODERATE) riskBadgeLabel.getStyleClass().add("badge-moderate");
        else riskBadgeLabel.getStyleClass().add("badge-low");

        // Indicator card values
        double waterDeficit = district.getWaterScarcity();
        double sanitationDeficit = 100.0 - district.getSanitationCoverage();
        scarcityValueLabel.setText(FormatUtil.formatPercentage(waterDeficit));
        sanitationValueLabel.setText(FormatUtil.formatPercentage(district.getSanitationCoverage()));
        densityValueLabel.setText(FormatUtil.formatDensity(district.getPopulationDensity()));
        waterProgressBar.setProgress(clamp(waterDeficit / 100.0));
        sanitationProgressBar.setProgress(clamp(district.getSanitationCoverage() / 100.0));
        densityProgressBar.setProgress(clamp(district.getPopulationDensity() / 1500.0));

        // Contribution chart mirrors the existing formula and 1500 people/km² density cap.
        updateContributionChart(waterDeficit, sanitationDeficit, district.getPopulationDensity());


        interpretationLabel.setText(generateInterpretationText(district));
    }

    /**
     * Mirrors the district's existing composite scoring: water 45%, sanitation 35%, density 20%.
     * Density contribution is normalised to 0–100 by capping at 1500 people/km².
     * This is a visualisation of the existing score, not a redefinition of it.
     */
    private void updateContributionChart(double waterDeficit, double sanitationDeficit,
                                         double populationDensity) {
        contributionChart.getData().clear();

        double waterContribution = waterDeficit * 0.45;
        double sanitationContribution = sanitationDeficit * 0.35;
        double densityIndex = Math.min(100.0, populationDensity / 15.0); // 1500 -> 100
        double densityContribution = densityIndex * 0.20;

        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.getData().add(component("Water (45%)", waterContribution));
        series.getData().add(component("Sanitation (35%)", sanitationContribution));
        series.getData().add(component("Density (20%)", densityContribution));

        contributionChart.getData().add(series);
    }

    private XYChart.Data<Number, String> component(String label, double contribution) {
        XYChart.Data<Number, String> data = new XYChart.Data<>(contribution, label);
        Tooltip tip = new Tooltip(String.format(Locale.ROOT,
                "%s weighted contribution: %.2f points", label, contribution));
        data.nodeProperty().addListener((obs, oldN, newN) -> {
            if (newN != null) Tooltip.install(newN, tip);
        });
        return data;
    }

    private double clamp(double value) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }


    private String generateInterpretationText(District d) {
        StringBuilder sb = new StringBuilder();
        sb.append("District ").append(d.getName())
          .append(" has a composite indicator score of ")
          .append(FormatUtil.formatScore(d.getRiskScore()))
          .append("/100, placing it in the ")
          .append(d.getRiskLevel().getLabel())
          .append(" tier under this model. ");

        double waterDeficit = d.getWaterScarcity();
        double sanitationCoverage = d.getSanitationCoverage();
        double sanitationDeficit = 100.0 - sanitationCoverage;

        if (d.getRiskLevel() == RiskLevel.HIGH) {
            sb.append("Primary risk drivers include a water-risk deficit of ")
              .append(FormatUtil.formatPercentage(waterDeficit))
              .append(" and a sanitation deficit of ")
              .append(FormatUtil.formatPercentage(sanitationDeficit))
              .append(", compounded by population density of ")
              .append(FormatUtil.formatDensity(d.getPopulationDensity()))
              .append(". These indicators support relative comparison and do not estimate disease cases.");
        } else if (d.getRiskLevel() == RiskLevel.MODERATE) {
            sb.append("The district shows moderate environmental exposure. Water-risk deficit is ")
              .append(FormatUtil.formatPercentage(waterDeficit))
              .append(" and sanitation coverage is ")
              .append(FormatUtil.formatPercentage(sanitationCoverage))
              .append(". The score is a comparative indicator, not a disease-incidence prediction.");
        } else {
            sb.append("The district maintains comparatively favourable environmental indicators, with water-risk deficit at ")
              .append(FormatUtil.formatPercentage(waterDeficit))
              .append(" and sanitation coverage at ")
              .append(FormatUtil.formatPercentage(sanitationCoverage))
              .append(". The score is a comparative indicator, not a disease-incidence prediction.");
        }
        return sb.toString();
    }
}