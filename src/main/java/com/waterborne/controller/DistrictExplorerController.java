package com.waterborne.controller;

import com.waterborne.model.District;
import com.waterborne.model.RiskLevel;
import com.waterborne.service.DistrictService;
import com.waterborne.util.AlertUtil;
import com.waterborne.util.FormatUtil;
import javafx.concurrent.Task;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
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
 * Handles district search list, custom cell rendering, progress bars, and automated risk interpretation.
 */
public class DistrictExplorerController implements Initializable, BaseController {

    @FXML private TextField searchField;
    @FXML private ListView<District> districtListView;

    @FXML private Label districtNameLabel;
    @FXML private Label riskScoreLabel;
    @FXML private Label riskBadgeLabel;

    @FXML private Label scarcityValueLabel;
    @FXML private ProgressBar scarcityProgressBar;

    @FXML private Label sanitationValueLabel;
    @FXML private ProgressBar sanitationProgressBar;

    @FXML private Label densityValueLabel;
    @FXML private ProgressBar densityProgressBar;

    @FXML private Label populationValueLabel;
    @FXML private Label interpretationLabel;
    @FXML private Label sourceComparisonLabel;

    private DistrictService districtService;
    private final ObservableList<District> masterDistricts = FXCollections.observableArrayList();
    private FilteredList<District> filteredDistricts;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupListView();
        setupSearchListener();
    }

    @Override
    public void initService(DistrictService service) {
        this.districtService = service;
        loadDistricts();
    }

    private void setupListView() {
        filteredDistricts = new FilteredList<>(masterDistricts, district -> true);
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
                    name.setWrapText(true);
                    name.setMaxWidth(190.0);
                    name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

                    Label state = new Label(item.getState() == null ? "" : item.getState());
                    state.setWrapText(true);
                    state.setMaxWidth(190.0);
                    state.getStyleClass().add("metric-subtext");
                    VBox districtLabels = new VBox(2.0, name, state);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label badge = new Label(item.getRiskLevel().getLabel());
                    badge.getStyleClass().add("badge");
                    badge.setMinWidth(Region.USE_PREF_SIZE);
                    if (item.getRiskLevel() == RiskLevel.HIGH) badge.getStyleClass().add("badge-high");
                    else if (item.getRiskLevel() == RiskLevel.MODERATE) badge.getStyleClass().add("badge-moderate");
                    else badge.getStyleClass().add("badge-low");

                    HBox container = new HBox(8.0, districtLabels, spacer, badge);
                    container.setAlignment(Pos.CENTER_LEFT);
                    container.setMaxWidth(Double.MAX_VALUE);
                    setGraphic(container);
                }
            }
        });

        districtListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                displayDistrictDetails(newSelection);
            }
        });
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applySearch(newValue));
    }

    private void loadDistricts() {
        if (districtService == null) return;
        Task<List<District>> task = new Task<>() {
            @Override
            protected List<District> call() throws Exception {
                return districtService.getAllDistricts();
            }
        };
        task.setOnSucceeded(event -> {
            masterDistricts.setAll(task.getValue());
            applySearch(searchField.getText());
            if (!masterDistricts.isEmpty()) {
                districtListView.getSelectionModel().selectFirst();
            }
        });
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            System.err.println("[ERROR] Failed to load districts: " + error.getMessage());
            AlertUtil.showError("Data Load Error", "Could Not Load Districts",
                    "Unable to load district data: " + error.getMessage());
        });

        Thread loader = new Thread(task, "district-explorer-data-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private void applySearch(String searchText) {
        String query = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);
        filteredDistricts.setPredicate(district ->
                query.isEmpty() || district.getName().toLowerCase(Locale.ROOT).contains(query));
        if (!filteredDistricts.isEmpty()) {
            districtListView.getSelectionModel().selectFirst();
        } else {
            districtListView.getSelectionModel().clearSelection();
        }
    }

    private void displayDistrictDetails(District district) {
        if (district == null) return;

        districtNameLabel.setText(district.getName());
        riskScoreLabel.setText("Score: " + FormatUtil.formatScore(district.getRiskScore()) + " / 100");

        RiskLevel level = district.getRiskLevel();
        riskBadgeLabel.setText(level.getLabel());
        riskBadgeLabel.getStyleClass().removeAll("badge-low", "badge-moderate", "badge-high");
        if (level == RiskLevel.HIGH) riskBadgeLabel.getStyleClass().add("badge-high");
        else if (level == RiskLevel.MODERATE) riskBadgeLabel.getStyleClass().add("badge-moderate");
        else riskBadgeLabel.getStyleClass().add("badge-low");

        // Progress Bars & Labels
        scarcityValueLabel.setText(FormatUtil.formatPercentage(district.getWaterScarcity()));
        scarcityProgressBar.setProgress(district.getWaterScarcity() / 100.0);

        sanitationValueLabel.setText(FormatUtil.formatPercentage(district.getSanitationCoverage()));
        sanitationProgressBar.setProgress(district.getSanitationCoverage() / 100.0);
        updateSourceComparison(district);

        densityValueLabel.setText(FormatUtil.formatDensity(district.getPopulationDensity()));
        double densityProgress = Math.min(1.0, district.getPopulationDensity() / 2000.0);
        densityProgressBar.setProgress(densityProgress);

        populationValueLabel.setText(FormatUtil.formatPopulation(district.getPopulation()) + " (" + String.format("%,d", district.getPopulation()) + " residents)");

        // Automated Risk Interpretation Summary Generation
        interpretationLabel.setText(generateInterpretationText(district));
    }

    private void updateSourceComparison(District district) {
        if (!Double.isFinite(district.getCensusWaterScarcity())
                || !Double.isFinite(district.getCensusSanitationCoverage())) {
            sourceComparisonLabel.setText("Census 2011 comparison metrics are not available for this data source.");
            return;
        }

        String censusValues = "Census 2011 proxies: water deficit "
                + FormatUtil.formatPercentage(district.getCensusWaterScarcity())
                + ", on-premises latrine coverage "
                + FormatUtil.formatPercentage(district.getCensusSanitationCoverage()) + ".";
        if (!district.hasNfhs5Metrics()) {
            sourceComparisonLabel.setText(censusValues
                    + " No matched NFHS-5 district record; risk uses Census values.");
            return;
        }

        sourceComparisonLabel.setText(censusValues + "\nNFHS-5 survey: improved drinking-water access "
                + FormatUtil.formatPercentage(district.getNfhsImprovedWaterCoverage())
                + ", improved sanitation "
                + FormatUtil.formatPercentage(district.getNfhsImprovedSanitationCoverage())
                + ". Risk uses NFHS-5 values for matched districts. Definitions differ, so these are comparisons, not identical measures.");
    }

    private String generateInterpretationText(District d) {
        StringBuilder sb = new StringBuilder();
        sb.append("District ").append(d.getName()).append(" presents an empirical waterborne disease risk score of ")
          .append(FormatUtil.formatScore(d.getRiskScore())).append("/100, placing it in the ").append(d.getRiskLevel().getLabel()).append(" tier. ");

        if (d.getRiskLevel() == RiskLevel.HIGH) {
            sb.append("Primary risk drivers include acute water scarcity (").append(FormatUtil.formatPercentage(d.getWaterScarcity()))
              .append(") and poor sanitation infrastructure (").append(FormatUtil.formatPercentage(d.getSanitationCoverage()))
              .append(" coverage), compounded by high population density (").append(FormatUtil.formatDensity(d.getPopulationDensity()))
              .append("). Immediate resource deployment for water purification and infrastructure intervention is strongly advised.");
        } else if (d.getRiskLevel() == RiskLevel.MODERATE) {
            sb.append("The district exhibits moderate environmental exposure. Water scarcity stands at ")
              .append(FormatUtil.formatPercentage(d.getWaterScarcity())).append(" with sanitation coverage at ")
              .append(FormatUtil.formatPercentage(d.getSanitationCoverage())).append(". Targeted sanitation upgrades and water conservation planning are recommended to prevent escalation to high risk.");
        } else {
            sb.append("The district maintains favorable environmental public health indicators, with relatively low water scarcity (")
              .append(FormatUtil.formatPercentage(d.getWaterScarcity())).append(") and robust sanitation coverage (")
              .append(FormatUtil.formatPercentage(d.getSanitationCoverage())).append("). Routine monitoring is advised.");
        }

        return sb.toString();
    }
}
